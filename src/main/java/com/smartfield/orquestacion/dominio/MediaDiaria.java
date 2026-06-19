package com.smartfield.orquestacion.dominio;

import java.time.LocalDate;

/** Punto agregado por dia de una serie temporal (InfluxDB). */
public record MediaDiaria(LocalDate dia, double valor) {
}
