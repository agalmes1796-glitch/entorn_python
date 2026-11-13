package com.videoteca.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topic de eventos: registra en log cada mensaje recibido.
 */
@Component
public class MessageConsumerListener {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerListener.class);

    @KafkaListener(
            topics = "${kafka.topic.events}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void onMessage(String message) {
        log.info("Mensaje recibido: {}", message);
    }
}
