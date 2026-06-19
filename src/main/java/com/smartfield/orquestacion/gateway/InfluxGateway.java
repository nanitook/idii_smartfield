package com.smartfield.orquestacion.gateway;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.smartfield.orquestacion.config.SmartfieldProperties;
import com.smartfield.orquestacion.dominio.MediaDiaria;
import com.smartfield.orquestacion.dominio.PicoTermico;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Acceso a InfluxDB (series temporales de sensores).
 *
 * Todas las consultas filtran por r._field == "valor" antes de cualquier
 * operacion numerica, porque cada medicion guarda tambien los fields de texto
 * "unidad" y "calidad", y sum()/mean()/comparaciones fallarian sobre strings.
 */
@Component
public class InfluxGateway {

    private final InfluxDBClient cliente;
    private final String bucket;

    public InfluxGateway(InfluxDBClient cliente, SmartfieldProperties props) {
        this.cliente = cliente;
        this.bucket = props.getInflux().getBucket();
    }

    /** Lluvia acumulada (mm) de un sensor en una ventana [desde, hasta]. */
    public double lluviaAcumulada(String sensor, Instant desde, Instant hasta) {
        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r.sensor == "%s" and r.variable == "precipitacion" and r._field == "valor")
                  |> sum()
                """.formatted(bucket, desde, hasta, sensor);
        return primerValor(flux).orElse(0.0);
    }

    /** Humedad de suelo media (%) de un sensor en los ultimos dias hasta una fecha. */
    public Double humedadMediaReciente(String sensor, Instant hasta, int dias) {
        Instant desde = hasta.minusSeconds((long) dias * 86_400);
        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r.sensor == "%s" and r.variable == "humedad_suelo" and r._field == "valor")
                  |> mean()
                """.formatted(bucket, desde, hasta, sensor);
        return primerValor(flux).orElse(null);
    }

    /** Media diaria de una variable de un sensor (aggregateWindow de 1 dia). */
    public List<MediaDiaria> mediasDiarias(String sensor, String variable, Instant desde, Instant hasta) {
        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r.sensor == "%s" and r.variable == "%s" and r._field == "valor")
                  |> aggregateWindow(every: 1d, fn: mean, createEmpty: false)
                """.formatted(bucket, desde, hasta, sensor, variable);
        List<MediaDiaria> salida = new ArrayList<>();
        for (FluxTable table : cliente.getQueryApi().query(flux)) {
            for (FluxRecord rec : table.getRecords()) {
                Object valor = rec.getValue();
                Instant t = rec.getTime();
                if (valor instanceof Number n && t != null) {
                    LocalDate dia = t.atZone(ZoneOffset.UTC).toLocalDate();
                    salida.add(new MediaDiaria(dia, n.doubleValue()));
                }
            }
        }
        return salida;
    }

    /**
     * Sumas diarias de una variable (ej. precipitacion), excluyendo dias en cero.
     * Util para reconstruir la serie de eventos de lluvia.
     */
    public List<MediaDiaria> sumasDiarias(String sensor, String variable, Instant desde, Instant hasta) {
        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r.sensor == "%s" and r.variable == "%s" and r._field == "valor")
                  |> aggregateWindow(every: 1d, fn: sum, createEmpty: false)
                  |> filter(fn: (r) => r._value > 0.0)
                """.formatted(bucket, desde, hasta, sensor, variable);
        List<MediaDiaria> salida = new ArrayList<>();
        for (FluxTable table : cliente.getQueryApi().query(flux)) {
            for (FluxRecord rec : table.getRecords()) {
                Object valor = rec.getValue();
                Instant t = rec.getTime();
                if (valor instanceof Number n && t != null) {
                    LocalDate dia = t.atZone(ZoneOffset.UTC).toLocalDate();
                    salida.add(new MediaDiaria(dia, n.doubleValue()));
                }
            }
        }
        return salida;
    }

    /**
     * Para cada dia con al menos una muestra de temperatura por encima del
     * umbral, devuelve el maximo del dia y la cantidad de muestras que lo
     * superaron. La consulta trae solo las muestras filtradas y se agrupa por
     * dia acá porque el dataset esperado de excedencias es chico.
     */
    public List<PicoTermico> picosTermicos(String sensor, double umbral, Instant desde, Instant hasta) {
        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r.sensor == "%s" and r.variable == "temperatura" and r._field == "valor")
                  |> filter(fn: (r) => r._value > %.2f)
                """.formatted(bucket, desde, hasta, sensor, umbral);
        Map<LocalDate, double[]> porDia = new TreeMap<>();   // dia -> [max, count]
        for (FluxTable table : cliente.getQueryApi().query(flux)) {
            for (FluxRecord rec : table.getRecords()) {
                Object valor = rec.getValue();
                Instant t = rec.getTime();
                if (valor instanceof Number n && t != null) {
                    LocalDate dia = t.atZone(ZoneOffset.UTC).toLocalDate();
                    double v = n.doubleValue();
                    porDia.compute(dia, (k, prev) -> {
                        if (prev == null) {
                            return new double[]{v, 1.0};
                        }
                        prev[0] = Math.max(prev[0], v);
                        prev[1] += 1.0;
                        return prev;
                    });
                }
            }
        }
        List<PicoTermico> salida = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> e : porDia.entrySet()) {
            salida.add(new PicoTermico(e.getKey(), e.getValue()[0], (int) e.getValue()[1]));
        }
        return salida;
    }

    /** Ejecuta una consulta que devuelve un unico escalar en _value. */
    private java.util.Optional<Double> primerValor(String flux) {
        for (FluxTable table : cliente.getQueryApi().query(flux)) {
            for (FluxRecord rec : table.getRecords()) {
                Object valor = rec.getValue();
                if (valor instanceof Number n) {
                    return java.util.Optional.of(n.doubleValue());
                }
            }
        }
        return java.util.Optional.empty();
    }
}
