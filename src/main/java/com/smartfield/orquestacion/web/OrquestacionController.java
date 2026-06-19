package com.smartfield.orquestacion.web;

import com.smartfield.orquestacion.dominio.CorrelacionLluvia;
import com.smartfield.orquestacion.dominio.EventoBiografia;
import com.smartfield.orquestacion.dominio.VecinoEnRiesgo;
import com.smartfield.orquestacion.dominio.ZonaCritica;
import com.smartfield.orquestacion.servicio.OrquestacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los patrones integrados como endpoints REST de demostracion.
 * <p>
 *   GET /api/patrones/vecinos-en-riesgo/lote-117?ventanaDias=14
 *   GET /api/patrones/correlacion-lluvia-plaga/lote-117?ventanaDias=5&umbralMm=25
 *   GET /api/patrones/biografia/lote-117
 *   GET /api/patrones/zona-critica/lote-117?ventanaDias=14&umbralHumedad=18
 */
@RestController
@RequestMapping("/api/patrones")
public class OrquestacionController {

    private final OrquestacionService servicio;

    public OrquestacionController(OrquestacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/vecinos-en-riesgo/{lote}")
    public List<VecinoEnRiesgo> vecinosEnRiesgo(
            @PathVariable String lote,
            @RequestParam(defaultValue = "14") int ventanaDias) {
        return servicio.vecinosEnRiesgo(lote, ventanaDias);
    }

    @GetMapping("/correlacion-lluvia-plaga/{lote}")
    public List<CorrelacionLluvia> correlacionLluviaPlaga(
            @PathVariable String lote,
            @RequestParam(defaultValue = "5") int ventanaDias,
            @RequestParam(defaultValue = "25") double umbralMm) {
        return servicio.correlacionLluviaPlaga(lote, ventanaDias, umbralMm);
    }

    @GetMapping("/biografia/{lote}")
    public List<EventoBiografia> biografia(@PathVariable String lote) {
        return servicio.biografiaLote(lote);
    }

    @GetMapping("/zona-critica/{lote}")
    public ZonaCritica zonaCritica(
            @PathVariable String lote,
            @RequestParam(defaultValue = "14") int ventanaDias,
            @RequestParam(defaultValue = "18") double umbralHumedad) {
        return servicio.deteccionZonaCritica(lote, ventanaDias, umbralHumedad);
    }
}
