package com.videoteca.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del microservicio de mensajería "kafka".
 *
 * <p>Microservicio basado en Spring Boot 4 y Apache Kafka: expone una API REST
 * para publicar mensajes en el topic de eventos y consume los mensajes recibidos.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaServiceApplication.class, args);
    }
}
