package com.videoteca.kafka;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la demo Kafka: escolta el topic {@link Mensatges#TOPIC} i
 * conserva els darrers {@value #MAX_MENSATGES} missatges rebuts en una llista
 * limitada i thread-safe, que el {@link MessageController} exposa per a la
 * consulta.
 *
 * <p>El bufer és efímer en memòria: es perd en cada reinici; el consumidor
 * retoma des de l'offset commitat al broker.</p>
 */
@Component
public class MessageListener {

    /** Nombre màxim de missatges conservats en memòria. */
    private static final int MAX_MENSATGES = 100;

    /** Últims missatges rebuts, del més recent al més antic. */
    private final Deque<String> ultimsMensatges = new ConcurrentLinkedDeque<>();

    /**
     * S'executa per a cada missatge rebut del topic.
     */
    @KafkaListener(topics = Mensatges.TOPIC)
    public void escolta(String contingut) {
        ultimsMensatges.addFirst(contingut);
        while (ultimsMensatges.size() > MAX_MENSATGES) {
            ultimsMensatges.removeLast();
        }
    }

    /**
     * Còpia immutable dels últims missatges rebuts (del més recent al més antic).
     */
    public List<String> ultims() {
        return List.copyOf(ultimsMensatges);
    }
}
