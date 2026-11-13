package com.videoteca.kafka.producer;

import com.videoteca.kafka.config.KafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de publicación de mensajes en Apache Kafka.
 */
@Service
public class MessageProducerService {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;

    public MessageProducerService(KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Publica un payload en el topic de eventos configurado.
     *
     * @param payload mensaje a publicar
     * @return futuro con el destino asignado (topic:partición) una vez confirmado el envío
     */
    public CompletableFuture<String> send(String payload) {
        String topic = properties.topic().events();
        log.info("Enviando mensaje al topic '{}': {}", topic, payload);
        return kafkaTemplate.send(topic, payload)
                .thenApply(record -> topic + ":" + record.getRecordMetadata().partition());
    }
}
