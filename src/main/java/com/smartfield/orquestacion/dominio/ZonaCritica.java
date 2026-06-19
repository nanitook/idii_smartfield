package com.smartfield.orquestacion.dominio;

import java.util.List;

/** Resultado del patron 13.4.4: zona critica detectada (cluster de lotes). */
public record ZonaCritica(
        List<String> lotes,
        String motivo,
        Double humedadMediaZona) {
}
