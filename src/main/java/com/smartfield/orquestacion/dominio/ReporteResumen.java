package com.smartfield.orquestacion.dominio;

import java.time.Instant;

/** Proyeccion liviana de un reporte documental (MongoDB). */
public record ReporteResumen(
        String id,
        String lote,
        Instant fecha,
        String categoria,
        String subcategoria,
        String severidad,
        String estado) {
}
