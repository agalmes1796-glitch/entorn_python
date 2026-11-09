package com.videoteca.frontend;

/**
 * Excepció de domini: el gateway (o el servei "movies" darrere seu) no pot
 * donar la llista de pel·lícules.
 *
 * <p>La llança {@link MoviesClient} quan la petició HTTP al gateway falla
 * (connexió rebutjada, timeout de connexió/lectura, resposta d'error del
 * gateway, ...). El controlador la converteix en HTTP
 * {@code 503 Service Unavailable} amb un JSON {@code {"error": "..."}}
 * descriptiu, sense fugar stack traces.</p>
 */
public class MoviesUnavailableException extends RuntimeException {

    public MoviesUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
