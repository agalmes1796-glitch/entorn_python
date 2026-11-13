package com.videoteca.kafka.controller;

import com.videoteca.kafka.producer.MessageProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * API REST del microservicio Kafka.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageProducerService producerService;

    public MessageController(MessageProducerService producerService) {
        this.producerService = producerService;
    }

    /** Cuerpo de la petición de envío. */
    public record NewMessage(String payload) {
    }

    /** Respuesta con el destino (topic:partición) y el mensaje enviado. */
    public record SendResult(String destination, String message) {
    }

    /**
     * Publica el payload recibido en el topic de eventos de Kafka.
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<SendResult>> send(@RequestBody NewMessage request) {
        return producerService.send(request.payload())
                .thenApply(destination -> ResponseEntity.ok(new SendResult(destination, request.payload())))
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(new SendResult("error", String.valueOf(ex.getMessage()))));
    }
}
