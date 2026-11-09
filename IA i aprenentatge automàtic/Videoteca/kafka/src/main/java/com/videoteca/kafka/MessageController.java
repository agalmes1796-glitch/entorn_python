package com.videoteca.kafka;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de la demo Kafka.
 *
 * <ul>
 *   <li>{@code POST /api/mensatges}: publica el missatge del cos al topic.</li>
 *   <li>{@code GET /api/mensatges}: retorna els últims missatges rebuts.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/mensatges")
public class MessageController {

    private final MessageProducer producer;
    private final MessageListener listener;

    public MessageController(MessageProducer producer, MessageListener listener) {
        this.producer = producer;
        this.listener = listener;
    }

    /**
     * Publica el missatge del cos de la petició al topic de la demo.
     */
    @PostMapping
    public MensatgePublicat publica(@RequestBody MensatgeRequest peticio) {
        if (peticio.contingut() == null || peticio.contingut().isBlank()) {
            throw new IllegalArgumentException("El camp 'contingut' és obligatori i no pot estar buit.");
        }
        producer.publica(peticio.contingut());
        return new MensatgePublicat(Mensatges.TOPIC, peticio.contingut());
    }

    /**
     * Retorna els últims missatges rebuts del topic (del més recent al més antic).
     */
    @GetMapping
    public List<String> ultims() {
        return listener.ultims();
    }

    /**
     * Peticions amb el camp {@code contingut} buit: error 400 amb missatge pla.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String error400(IllegalArgumentException excepcio) {
        return excepcio.getMessage();
    }
}
