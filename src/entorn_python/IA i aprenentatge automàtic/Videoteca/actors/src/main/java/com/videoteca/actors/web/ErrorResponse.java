package com.videoteca.actors.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cos d'error estàndard de l'API: la mateixa forma per a tots els codis
 * d'error (400, 404, 500...), de manera que el client pugui processar-lo
 * uniformement.
 *
 * @param timestamp instant en què s'ha produït l'error (UTC)
 * @param status    codi HTTP de l'error
 * @param error     raó HTTP estàndard (p. ex. "Not Found")
 * @param message   missatge de l'error en llenguatge natural
 * @param path      URI de la petició que ha causat l'error
 * @param errors    camps afectats en errors de validació (o {@code null} si
 *                  no n'hi ha; llavors el camp no es serialitza)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors) {

    /**
     * Error d'un camp concret de la petició (errors de validació, codi 400).
     *
     * @param field   nom del camp afectat
     * @param message descripció de la violació
     */
    public record FieldError(String field, String message) {
    }
}
