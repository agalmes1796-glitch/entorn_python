package com.videoteca.notifications.api.web;

/**
 * Cuerpo de error 404: {@code {"error": "Not Found", "message": ..., "path": ...}}.
 */
public record NotFoundErrorResponse(String error, String message, String path) {

}
