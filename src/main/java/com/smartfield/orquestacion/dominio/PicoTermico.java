package com.smartfield.orquestacion.dominio;

import java.time.LocalDate;

/**
 * Resumen diario de muestras de temperatura por encima de un umbral:
 * cuantas muestras lo superaron y cual fue el maximo del dia.
 */
public record PicoTermico(LocalDate dia, double maxValor, int cantidad) {
}
