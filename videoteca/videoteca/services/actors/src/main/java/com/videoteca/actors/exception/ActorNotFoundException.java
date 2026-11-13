package com.videoteca.actors.exception;

/**
 * Se lanza cuando un actor no existe en la base de datos.
 * Se traduce a HTTP 404 en {@link com.videoteca.actors.web.GlobalExceptionHandler}.
 */
public class ActorNotFoundException extends RuntimeException {

    public ActorNotFoundException(String message) {
        super(message);
    }
}
