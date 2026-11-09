package com.videoteca.kafka;

/**
 * Resposta de la publicació d'un missatge: el topic i el missatge enviat.
 */
public record MensatgePublicat(String topic, String contingut) {
}
