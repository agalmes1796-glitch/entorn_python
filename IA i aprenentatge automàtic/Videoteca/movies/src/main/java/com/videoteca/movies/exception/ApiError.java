package com.videoteca.movies.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo de error coherente para todas las respuestas 4xx/5xx de la API.
 *
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Pelicula con id 999 no encontrada",
 *   "timestamp": "2026-11-06T10:00:00.123Z",
 *   "field_errors": { "titulo": "El título no puede ser vacío ni nulo" }
 * }
 * </pre>
 *
 * <p>El {@code timestamp} es un {@link Instant} (UTC, ISO-8601); según la
 * precisión del reloj incluye milisegundos (o microsegundos):
 * {@code 2026-11-06T10:00:00.123Z}.
 *
 * <p><b>Nomenclatura del JSON (contrato frontend):</b> los componentes del
 * record se serializan en snake_case por la naming strategy global de Jackson
 * ({@code spring.jackson.property-naming-strategy: SNAKE_CASE} en
 * {@code application.yml}), por lo que el campo sale como
 * {@code field_errors} (no {@code fieldErrors}). Las <b>claves del mapa</b>
 * {@link #fieldErrors} NO se renuevan (una naming strategy no toca las claves
 * de un {@code Map}): son los nombres de propiedad Java en camelCase
 * ({@code titulo}, {@code generoId}, {@code anioEstreno}, ...), igual que en
 * la validación de {@link org.springframework.validation.FieldError}.
 */
public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    /**
     * Fábrica con timestamp actual.
     *
     * @param fieldErrors errores por campo; <b>nunca {@code null}</b>: si no hay
     *                    errores de validación se pasa un {@link Map} vacío
     *                    ({@code Map.of()})
     */
    public static ApiError of(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, error, message, Instant.now(), fieldErrors);
    }
}
