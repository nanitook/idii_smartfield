package com.smartfield.orquestacion;

import com.smartfield.orquestacion.config.SmartfieldProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Capa de orquestacion de SmartField.
 *
 * Coordina los tres motores para resolver los patrones integrados que
 * ningun motor puede responder por si solo. La integracion se apoya en los
 * identificadores compartidos id_lote e id_sensor.
 */
@SpringBootApplication
@EnableConfigurationProperties(SmartfieldProperties.class)
public class SmartfieldOrquestacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartfieldOrquestacionApplication.class, args);
    }
}
