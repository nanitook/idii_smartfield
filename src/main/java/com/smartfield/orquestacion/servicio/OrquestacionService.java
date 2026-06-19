package com.smartfield.orquestacion.servicio;

import com.smartfield.orquestacion.config.SmartfieldProperties;
import com.smartfield.orquestacion.dominio.CorrelacionLluvia;
import com.smartfield.orquestacion.dominio.EventoBiografia;
import com.smartfield.orquestacion.dominio.MediaDiaria;
import com.smartfield.orquestacion.dominio.PicoTermico;
import com.smartfield.orquestacion.dominio.ReporteResumen;
import com.smartfield.orquestacion.dominio.Vecino;
import com.smartfield.orquestacion.dominio.VecinoEnRiesgo;
import com.smartfield.orquestacion.dominio.ZonaCritica;
import com.smartfield.orquestacion.gateway.InfluxGateway;
import com.smartfield.orquestacion.gateway.MongoGateway;
import com.smartfield.orquestacion.gateway.Neo4jGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Patrones integrados (seccion 13.4 del doc). Cada metodo coordina los tres
 * motores, ninguno se resuelve dentro de un unico motor. La integracion se apoya
 * en los identificadores compartidos id_lote e id_sensor.
 */
@Service
public class OrquestacionService {

    private static final DateTimeFormatter F_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Neo4jGateway neo4j;
    private final MongoGateway mongo;
    private final InfluxGateway influx;
    private final Instant ahora;

    public OrquestacionService(Neo4jGateway neo4j, MongoGateway mongo,
                               InfluxGateway influx, SmartfieldProperties props) {
        this.neo4j = neo4j;
        this.mongo = mongo;
        this.influx = influx;
        this.ahora = props.fechaReferenciaComoInstant();
    }

    /**
     * 13.4.1
     * Vecinos en riesgo (flujo completo): lote critico -> vecinos (Neo4j) ->
     * reportes recientes (MongoDB) -> estado ambiental (InfluxDB) -> vista
     * integrada. Un vecino se marca en riesgo si tiene reportes recientes.
     */
    public List<VecinoEnRiesgo> vecinosEnRiesgo(String loteCritico, int ventanaDias) {
        Instant desde = ahora.minusSeconds((long) ventanaDias * 86_400);
        List<Vecino> vecinos = neo4j.vecinosDirectos(loteCritico); // (1) Neo4j

        List<VecinoEnRiesgo> vista = new ArrayList<>();
        for (Vecino v : vecinos) {
            List<ReporteResumen> recientes = mongo.reportesDesde(v.lote(), desde); // (2) MongoDB
            Double humedad = (v.sensor() != null) // (3) InfluxDB
                    ? influx.humedadMediaReciente(v.sensor(), ahora, ventanaDias)
                    : null;
            List<String> detalle = recientes.stream()
                    .map(r -> r.id() + " (" + r.categoria() + ", " + r.severidad() + ")")
                    .toList();
            vista.add(new VecinoEnRiesgo(
                    v.lote(), v.sensor(), recientes.size(), detalle, humedad,
                    !recientes.isEmpty())); // (4) regla de riesgo
        }
        // prioriza los de mayor cantidad de reportes recientes
        vista.sort(Comparator.comparingInt(VecinoEnRiesgo::reportesRecientes).reversed());
        return vista;
    }

    /**
     * 13.4.2
     * Correlacion lluvia-plaga: para cada reporte de plaga del lote, suma la
     * lluvia (InfluxDB) en la ventana de dias previos y lo marca correlacionado
     * si supera el umbral en milimetros.
     */
    public List<CorrelacionLluvia> correlacionLluviaPlaga(String loteId, int ventanaDiasPrevios,
                                                          double umbralMm) {
        String sensor = sensorDe(loteId);
        List<ReporteResumen> plagas = mongo.reportesDePlaga(loteId);   // MongoDB
        List<CorrelacionLluvia> salida = new ArrayList<>();
        for (ReporteResumen r : plagas) {
            Instant hasta = r.fecha();
            Instant desde = hasta.minusSeconds((long) ventanaDiasPrevios * 86_400);
            double lluvia = (sensor != null) ? influx.lluviaAcumulada(sensor, desde, hasta) : 0.0; // InfluxDB
            salida.add(new CorrelacionLluvia(
                    r.id(), r.fecha(), r.severidad(),
                    redondear(lluvia), lluvia >= umbralMm));
        }
        return salida;
    }

    /**
     * 13.4.3
     * Biografia cronologica del lote: fusiona en una unica linea temporal los
     * reportes (MongoDB) con cuatro tipos de eventos derivados de las series
     * temporales (InfluxDB): temperatura media diaria, lluvia diaria, dias en
     * los que la humedad de suelo media perfora el piso del 18 % y dias con
     * muestras de temperatura por encima de 35 C (golpe de calor). El resultado
     * permite leer en orden la "ventana humeda inicial", la aparicion y
     * escalada de la plaga y el deterioro ambiental (sequia mas pico termico)
     * que acompaño el agravamiento.
     */
    public List<EventoBiografia> biografiaLote(String loteId) {
        List<EventoBiografia> eventos = new ArrayList<>();

        // (a) Reportes documentales (MongoDB)
        for (ReporteResumen r : mongo.reportesPorLote(loteId)) {
            eventos.add(new EventoBiografia(r.fecha(), "REPORTE",
                    "%s - %s (%s)".formatted(r.id(), r.categoria(), r.severidad())));
        }

        String sensor = sensorDe(loteId);
        if (sensor != null) {
            Instant desde = Instant.parse("2026-05-01T00:00:00Z");
            java.time.ZoneOffset utc = java.time.ZoneOffset.UTC;

            // (b) Temperatura media diaria (InfluxDB)
            for (MediaDiaria m : influx.mediasDiarias(sensor, "temperatura", desde, ahora)) {
                eventos.add(new EventoBiografia(
                        m.dia().atStartOfDay().toInstant(utc),
                        "MEDICION",
                        "Temp. media %.1f C".formatted(m.valor())));
            }

            // (c) Lluvia diaria > 0 mm (InfluxDB)
            for (MediaDiaria d : influx.sumasDiarias(sensor, "precipitacion", desde, ahora)) {
                eventos.add(new EventoBiografia(
                        d.dia().atStartOfDay().toInstant(utc),
                        "MEDICION",
                        "Lluvia diaria %.1f mm".formatted(d.valor())));
            }

            // (d) Dias con humedad de suelo media por debajo del 18 % (InfluxDB)
            for (MediaDiaria m : influx.mediasDiarias(sensor, "humedad_suelo", desde, ahora)) {
                if (m.valor() < 18.0) {
                    eventos.add(new EventoBiografia(
                            m.dia().atStartOfDay().toInstant(utc),
                            "MEDICION",
                            "Humedad de suelo %.1f%% (bajo umbral 18%%)".formatted(m.valor())));
                }
            }

            // (e) Picos termicos: muestras de temperatura > 35 C (InfluxDB)
            for (PicoTermico p : influx.picosTermicos(sensor, 35.0, desde, ahora)) {
                eventos.add(new EventoBiografia(
                        p.dia().atStartOfDay().toInstant(utc),
                        "MEDICION",
                        "Pico termico: maxima %.2f C (%d muestras > 35 C)"
                                .formatted(p.maxValor(), p.cantidad())));
            }
        }
        eventos.sort(Comparator.comparing(EventoBiografia::fecha));
        return eventos;
    }

    /**
     * 13.4.4
     * Deteccion de zonas criticas: parte de los lotes con acumulacion de
     * incidentes (MongoDB), se queda con el cluster contiguo que contiene al
     * lote semilla (Neo4j) y confirma un ambiente seco compartido (InfluxDB).
     */
    public ZonaCritica deteccionZonaCritica(String loteSemilla, int ventanaDias,
                                            double umbralHumedadSeca) {
        Instant desde = ahora.minusSeconds((long) ventanaDias * 86_400);

        // (1) MongoDB: lotes con al menos un reporte de plaga reciente
        List<String> candidatos = mongo.lotesConPlagaReciente(desde);

        // (2) Neo4j: cluster contiguo que contiene a la semilla
        List<String> cluster = neo4j.componenteConexo(loteSemilla, candidatos);
        if (cluster.isEmpty()) {
            return new ZonaCritica(List.of(), "Sin zona contigua para " + loteSemilla, null);
        }

        // (3) InfluxDB: humedad media de la zona (confirma racha seca compartida)
        Map<String, String> sensores = neo4j.sensoresDe(cluster);
        double suma = 0;
        int n = 0;
        for (String lote : cluster) {
            String sensor = sensores.get(lote);
            if (sensor != null) {
                Double h = influx.humedadMediaReciente(sensor, ahora, ventanaDias);
                if (h != null) { suma += h; n++; }
            }
        }
        Double humedadZona = (n > 0) ? redondear(suma / n) : null;
        String motivo = "Lotes contiguos con incidentes recientes"
                + (humedadZona != null && humedadZona < umbralHumedadSeca
                    ? " bajo racha seca (humedad media " + humedadZona + " %)"
                    : "");
        return new ZonaCritica(cluster, motivo, humedadZona);
    }

    private String sensorDe(String loteId) {
        return neo4j.sensoresDe(List.of(loteId)).get(loteId);
    }

    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public static String formatearDia(Instant i) {
        return F_DIA.format(i.atZone(java.time.ZoneOffset.UTC));
    }
}
