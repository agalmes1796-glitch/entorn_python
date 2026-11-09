package com.videoteca.notifications.exception;

import java.util.UUID;

/**
 * Excepció del domini llançada quan una notificació no existeix.
 *
 * <p>Es mapa a HTTP 404 {@code Not Found} pel {@code GlobalExceptionHandler}.</p>
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notificació no trobada amb l'id: " + id);
    }
}
