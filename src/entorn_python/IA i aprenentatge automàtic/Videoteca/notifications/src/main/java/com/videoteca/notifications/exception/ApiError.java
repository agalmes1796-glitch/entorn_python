package com.videoteca.notifications.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Cos JSON estàndard dels errors d'API.
 *
 * <p>{@code field} només s'omple quan l'error prové d'un camp invàlid
 * de la petició (400); per a altres errors (p. ex., 404) es serialitza
 * sense el camp (vegeu {@link JsonInclude}).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String field
) {
}
