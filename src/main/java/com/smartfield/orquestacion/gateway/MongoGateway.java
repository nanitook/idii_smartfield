package com.smartfield.orquestacion.gateway;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartfield.orquestacion.dominio.ReporteResumen;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Accumulators.max;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.descending;

/**
 * Acceso a MongoDB (reportes documentales de incidentes).
 */
@Component
public class MongoGateway {

    private final MongoCollection<Document> reportes;

    public MongoGateway(MongoDatabase db) {
        this.reportes = db.getCollection("reportes");
    }

    /** Reportes de un lote, del mas reciente al mas antiguo. */
    public List<ReporteResumen> reportesPorLote(String loteId) {
        List<ReporteResumen> salida = new ArrayList<>();
        reportes.find(eq("lote", loteId))
                .sort(descending("timestamp_evento"))
                .forEach(d -> salida.add(aResumen(d)));
        return salida;
    }

    /** Reportes de un lote a partir de una fecha (ventana reciente). */
    public List<ReporteResumen> reportesDesde(String loteId, Instant desde) {
        List<ReporteResumen> salida = new ArrayList<>();
        reportes.find(and(eq("lote", loteId), gte("timestamp_evento", Date.from(desde))))
                .sort(descending("timestamp_evento"))
                .forEach(d -> salida.add(aResumen(d)));
        return salida;
    }

    /** Reportes de plaga de un lote (todos), ascendente por fecha. */
    public List<ReporteResumen> reportesDePlaga(String loteId) {
        List<ReporteResumen> salida = new ArrayList<>();
        reportes.find(and(eq("lote", loteId), eq("categoria", "plaga")))
                .sort(com.mongodb.client.model.Sorts.ascending("timestamp_evento"))
                .forEach(d -> salida.add(aResumen(d)));
        return salida;
    }

    /**
     * Acumulacion de incidentes de severidad alta o critica por lote desde una
     * fecha, quedandose con los lotes que igualan o superan el umbral.
     * Devuelve lote -> cantidad.
     */
    public Map<String, Integer> acumulacionCriticos(Instant desde, int umbral) {
        List<Bson> pipeline = List.of(
                match(and(in("severidad", "alta", "critica"),
                          gte("timestamp_evento", Date.from(desde)))),
                group("$lote", sum("total", 1), max("ultimo", "$timestamp_evento")),
                match(gte("total", umbral)),
                sort(descending("total")));
        Map<String, Integer> salida = new LinkedHashMap<>();
        reportes.aggregate(pipeline)
                .forEach(d -> salida.put(d.getString("_id"), d.getInteger("total")));
        return salida;
    }

    /** Lotes distintos que tienen al menos un reporte de plaga desde una fecha. */
    public List<String> lotesConPlagaReciente(Instant desde) {
        List<String> salida = new ArrayList<>();
        reportes.distinct("lote",
                          and(eq("categoria", "plaga"), gte("timestamp_evento", Date.from(desde))),
                          String.class)
                .forEach(salida::add);
        return salida;
    }

    private ReporteResumen aResumen(Document d) {
        Date ts = d.getDate("timestamp_evento");
        return new ReporteResumen(
                d.getString("_id"),
                d.getString("lote"),
                ts != null ? ts.toInstant() : null,
                d.getString("categoria"),
                d.getString("subcategoria"),
                d.getString("severidad"),
                d.getString("estado"));
    }
}
