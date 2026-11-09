package com.videoteca.frontend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Conversió dels errors de la petició al gateway en un 503 (Service Unavailable).
 *
 * <p>Captura qualsevol fallada del client HTTP (gateway no disponible, servei
 * "movies" no registrat, timeout, ...). El cos de la resposta és un JSON
 * descriptiu però genèric: els detalls tècnics (stack trace) només s'escriuen
 * al log del servidor, mai a la resposta.</p>
 */
@RestControllerAdvice
public class GatewayErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorAdvice.class);

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError gatewayNoDisponible(RestClientException exc) {
        log.warn("No s'ha pogut obtenir la llista de pel·lícules del gateway", exc);
        return new ApiError("El servei 'movies' no és disponible: el gateway o el servei de pel·lícules "
                + "no ha pogut respondre la petició. Intenteu-ho de nou més tard.");
    }
}
