package com.smartfield.orquestacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;

/**
 * Las cosas quedan hardcodeadas porque es para correr de forma local
 */
@ConfigurationProperties(prefix = "smartfield")
public class SmartfieldProperties {

    private String fechaReferencia = "2026-05-31T23:59:59Z";
    private boolean demo = true;
    private Mongo mongo = new Mongo();
    private Neo4j neo4j = new Neo4j();
    private Influx influx = new Influx();

    /** "Ahora" de referencia para reproducir el dataset historico. */
    public Instant fechaReferenciaComoInstant() {
        return Instant.parse(fechaReferencia);
    }

    public static class Mongo {
        private String uri = "mongodb://localhost:27017";
        private String baseDatos = "smartfield";
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getBaseDatos() { return baseDatos; }
        public void setBaseDatos(String baseDatos) { this.baseDatos = baseDatos; }
    }

    public static class Neo4j {
        private String uri = "bolt://localhost:7687";
        private String usuario = "neo4j";
        private String clave = "smartfield123";
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getClave() { return clave; }
        public void setClave(String clave) { this.clave = clave; }
    }

    public static class Influx {
        private String url = "http://localhost:8086";
        private String token = "smartfield-token-dev";
        private String org = "smartfield";
        private String bucket = "mediciones";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getOrg() { return org; }
        public void setOrg(String org) { this.org = org; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
    }

    public String getFechaReferencia() { return fechaReferencia; }
    public void setFechaReferencia(String fechaReferencia) { this.fechaReferencia = fechaReferencia; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public Mongo getMongo() { return mongo; }
    public void setMongo(Mongo mongo) { this.mongo = mongo; }
    public Neo4j getNeo4j() { return neo4j; }
    public void setNeo4j(Neo4j neo4j) { this.neo4j = neo4j; }
    public Influx getInflux() { return influx; }
    public void setInflux(Influx influx) { this.influx = influx; }
}
