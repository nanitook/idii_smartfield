package com.smartfield.orquestacion.dominio;

import java.time.Instant;

/** Resultado del patron 13.4.2: correlacion entre lluvia previa y un reporte de plaga. */
public record CorrelacionLluvia(
        String reporteId,
        Instant fecha,
        String severidad,
        double lluviaAcumuladaPrevia,
        boolean correlacionado) {
}
