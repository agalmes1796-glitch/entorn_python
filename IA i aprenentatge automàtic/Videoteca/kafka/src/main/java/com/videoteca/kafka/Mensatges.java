package com.videoteca.kafka;

/**
 * Constants del topic de la demo Kafka.
 *
 * <p>El topic {@code videoteca-mensatges} és compartit pel
 * {@link MessageProducer} (publicació) i pel {@link MessageListener}
 * (escolta); el centralitza aquí perquè ambdues classes el comparteixin.</p>
 */
public final class Mensatges {

    /** Nom del topic de la demo. */
    public static final String TOPIC = "videoteca-mensatges";

    private Mensatges() {
    }
}
