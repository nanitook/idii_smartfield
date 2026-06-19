package com.smartfield.orquestacion.dominio;

import java.time.Instant;

/** Entrada de la linea temporal del patron 13.4.3 (biografia cronologica del lote). */
public record EventoBiografia(
        Instant fecha,
        String tipo,        // "REPORTE" | "MEDICION"
        String descripcion) {
}
