package com.smartfield.orquestacion;

import com.smartfield.orquestacion.config.SmartfieldProperties;
import com.smartfield.orquestacion.dominio.CorrelacionLluvia;
import com.smartfield.orquestacion.dominio.EventoBiografia;
import com.smartfield.orquestacion.dominio.VecinoEnRiesgo;
import com.smartfield.orquestacion.dominio.ZonaCritica;
import com.smartfield.orquestacion.servicio.OrquestacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar (si smartfield.demo=true) ejecuta los cuatro patrones integrados
 * sobre el lote-117 e imprime los resultados por consola. Equivale a la
 * demostracion de la seccion 13.4 del informe.
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);
    private static final String LOTE = "lote-117";

    private final OrquestacionService servicio;
    private final boolean habilitado;

    public DemoRunner(OrquestacionService servicio, SmartfieldProperties props) {
        this.servicio = servicio;
        this.habilitado = props.isDemo();
    }

    @Override
    public void run(String... args) {
        if (!habilitado) {
            return;
        }
        try {
            demo();
        } catch (Exception e) {
            log.warn("No se pudo ejecutar la demo (¿estan los tres motores levantados " +
                    "y con datos cargados?): {}", e.getMessage());
        }
    }

    private void demo() {
        log.info("===== 13.4.1  Vecinos en riesgo de {} =====", LOTE);
        for (VecinoEnRiesgo v : servicio.vecinosEnRiesgo(LOTE, 14)) {
            log.info("  {} | reportes={} {} | humedad media={}% | en_riesgo={}",
                    v.lote(), v.reportesRecientes(), v.detalleReportes(),
                    v.humedadMediaReciente(), v.enRiesgo());
        }

        log.info("===== 13.4.2  Correlacion lluvia-plaga de {} =====", LOTE);
        for (CorrelacionLluvia c : servicio.correlacionLluviaPlaga(LOTE, 5, 25)) {
            log.info("  {} | {} | {} | lluvia 5d previos={} mm | correlacionado={}",
                    c.reporteId(), OrquestacionService.formatearDia(c.fecha()),
                    c.severidad(), c.lluviaAcumuladaPrevia(), c.correlacionado());
        }

        log.info("===== 13.4.3  Biografia cronologica de {} =====", LOTE);
        List<EventoBiografia> bio = servicio.biografiaLote(LOTE);
        for (EventoBiografia e : bio) {
            log.info("  {} | {} | {}",
                    OrquestacionService.formatearDia(e.fecha()), e.tipo(), e.descripcion());
        }

        log.info("===== 13.4.4  Zona critica desde {} =====", LOTE);
        ZonaCritica zona = servicio.deteccionZonaCritica(LOTE, 14, 18);
        log.info("  lotes={} | motivo={} | humedad media zona={}%",
                zona.lotes(), zona.motivo(), zona.humedadMediaZona());
    }
}
