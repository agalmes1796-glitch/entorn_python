package com.videoteca.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservici Kafka del projecte Videoteca (demo de productor/consumidor).
 *
 * <p>Publica i escolta missatges de text al topic {@code videoteca-mensatges}
 * (vegeu {@link Mensatges}); es registra al servidor Eureka perquè el gateway
 * pugui redirigir-hi les peticions (vegeu {@code application.yml}).</p>
 */
@SpringBootApplication
public class KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
    }
}
