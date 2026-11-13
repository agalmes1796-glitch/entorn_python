package com.videoteca.movies.exception;

/**
 * Se lanza cuando una película no existe en la base de datos.
 * Se traduce a HTTP 404 en {@link GlobalExceptionHandler}.
 */
public class MovieNoEncontradaException extends RuntimeException {

    public MovieNoEncontradaException(String message) {
        super(message);
    }
}
