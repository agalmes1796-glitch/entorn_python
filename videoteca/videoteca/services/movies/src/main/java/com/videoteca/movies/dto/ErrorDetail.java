package com.videoteca.movies.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo JSON de error de la API.
 *
 * <p>Formato:
 * <pre>{@code
 * {
 *   "timestamp": "2026-11-06T10:15:30Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "path": "/api/v1/movies/42",
 *   "message": "Pelicula con id 42 no encontrada",
 *   "errors": { "title": "El título es obligatorio" }   // opcional
 * }
 * }</pre>
 */
public record ErrorDetail(
        Instant timestamp,
        int status,
        String error,
        String path,
        String message,
        Map<String, String> errors
) {
}
