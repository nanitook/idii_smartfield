package com.smartfield.orquestacion.gateway;

import com.smartfield.orquestacion.dominio.Vecino;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acceso a Neo4j (topologia territorial). Resuelve vecindad, propagacion y
 * componentes conexos entre lotes. Se usan transacciones de autocommit
 * (session.run) por tratarse de consultas de lectura simples.
 */
@Component
public class Neo4jGateway {

    private final Driver driver;

    public Neo4jGateway(Driver driver) {
        this.driver = driver;
    }

    /** Vecinos directos (1 salto) de un lote, con su sensor asociado. */
    public List<Vecino> vecinosDirectos(String loteId) {
        String cypher = """
                MATCH (l:Lote {id: $id})-[:ADYACENTE_A]-(v:Lote)
                RETURN v.id AS lote, v.sensor AS sensor
                ORDER BY v.id
                """;
        try (Session session = driver.session()) {
            return session.run(cypher, Values.parameters("id", loteId))
                    .list(r -> new Vecino(r.get("lote").asString(),
                                          r.get("sensor").asString(null)));
        }
    }

    /** Mapa lote -> sensor para un conjunto de lotes. */
    public Map<String, String> sensoresDe(Collection<String> lotes) {
        String cypher = """
                MATCH (l:Lote) WHERE l.id IN $lotes
                RETURN l.id AS lote, l.sensor AS sensor
                """;
        Map<String, String> salida = new HashMap<>();
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("lotes", new ArrayList<>(lotes)))
                    .forEachRemaining(r -> salida.put(r.get("lote").asString(),
                                                      r.get("sensor").asString(null)));
        }
        return salida;
    }

    /**
     * Dentro de un conjunto de lotes candidatos, devuelve el componente conexo
     * (por ADYACENTE_A) que contiene al lote semilla. Sirve para delimitar una
     * zona critica contigua. El recorrido se hace acá a partir de las
     * aristas internas que devuelve Neo4j.
     */
    public List<String> componenteConexo(String semilla, Collection<String> candidatos) {
        if (!candidatos.contains(semilla)) {
            return new ArrayList<>();
        }
        String cypher = """
                MATCH (a:Lote)-[:ADYACENTE_A]-(b:Lote)
                WHERE a.id IN $lotes AND b.id IN $lotes
                RETURN a.id AS a, b.id AS b
                """;
        Map<String, Set<String>> ady = new HashMap<>();
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("lotes", new ArrayList<>(candidatos)))
                    .forEachRemaining(r -> {
                        String a = r.get("a").asString();
                        String b = r.get("b").asString();
                        ady.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                        ady.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                    });
        }
        // BFS desde la semilla sobre el subgrafo inducido por los candidatos
        List<String> componente = new ArrayList<>();
        Set<String> visitados = new HashSet<>();
        Deque<String> cola = new ArrayDeque<>();
        cola.add(semilla);
        visitados.add(semilla);
        while (!cola.isEmpty()) {
            String actual = cola.poll();
            componente.add(actual);
            for (String vecino : ady.getOrDefault(actual, Set.of())) {
                if (visitados.add(vecino)) {
                    cola.add(vecino);
                }
            }
        }
        componente.sort(String::compareTo);
        return componente;
    }
}
