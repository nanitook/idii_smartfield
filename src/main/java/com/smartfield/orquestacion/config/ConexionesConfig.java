package com.smartfield.orquestacion.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConexionesConfig {

    private final SmartfieldProperties props;

    public ConexionesConfig(SmartfieldProperties props) {
        this.props = props;
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        return MongoClients.create(props.getMongo().getUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient cliente) {
        return cliente.getDatabase(props.getMongo().getBaseDatos());
    }

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver() {
        return GraphDatabase.driver(
                props.getNeo4j().getUri(),
                AuthTokens.basic(props.getNeo4j().getUsuario(), props.getNeo4j().getClave()));
    }

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(
                props.getInflux().getUrl(),
                props.getInflux().getToken().toCharArray(),
                props.getInflux().getOrg(),
                props.getInflux().getBucket());
    }
}
