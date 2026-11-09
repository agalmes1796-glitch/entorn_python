package com.videoteca.kafka;

/**
 * Cos de la petició {@code POST /api/mensatges}: el contingut a publicar.
 */
public record MensatgeRequest(String contingut) {
}
