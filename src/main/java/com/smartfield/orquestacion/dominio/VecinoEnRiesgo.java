package com.smartfield.orquestacion.dominio;

import java.util.List;

/** Resultado del patron 13.4.1: vista integrada de riesgo de un lote vecino. */
public record VecinoEnRiesgo(
        String lote,
        String sensor,
        int reportesRecientes,
        List<String> detalleReportes,
        Double humedadMediaReciente,
        boolean enRiesgo) {
}
