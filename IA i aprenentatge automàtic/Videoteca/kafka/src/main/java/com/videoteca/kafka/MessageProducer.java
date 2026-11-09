package com.videoteca.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de la demo Kafka: publica missatges de text al topic
 * {@link Mensatges#TOPIC}.
 */
@Component
public class MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica el missatge donat al topic {@link Mensatges#TOPIC}.
     *
     * <p>L'enviament és asíncron: {@link KafkaTemplate#send} retorna un futur
     * que es resol quan el broker confirma el missatge; la demo no el
     * verifica, per simplicitat.</p>
     */
    public void publica(String contingut) {
        kafkaTemplate.send(Mensatges.TOPIC, contingut);
    }
}
