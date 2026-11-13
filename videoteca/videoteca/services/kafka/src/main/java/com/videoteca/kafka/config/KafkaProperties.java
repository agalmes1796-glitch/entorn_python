package com.videoteca.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del microservicio Kafka.
 *
 * <p>Se cargan desde el bloque {@code kafka:} de {@code application.yml}
 * (ver también la variable de entorno {@code KAFKA_BOOTSTRAP_SERVERS}).</p>
 */
@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(
        String bootstrapServers,
        Topic topic,
        Consumer consumer,
        Producer producer
) {

    public record Topic(
            /** Topic de eventos al que publica el producer y que consume el listener. */
            String events
    ) {
    }

    public record Consumer(
            /** Grupo de consumo para el listener de eventos. */
            String groupId
    ) {
    }

    public record Producer(
            /** Política de confirmación (por ejemplo "all"). */
            String acks,
            /** Número de reintentos ante fallos de envío. */
            int retries
    ) {
    }
}
