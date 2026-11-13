package com.videoteca.notifications.api.web;

/**
 * Cuerpo de error 409: {@code {"error": "Conflict", "message": ..., "path": ...}}.
 */
public record ConflictErrorResponse(String error, String message, String path) {

}
