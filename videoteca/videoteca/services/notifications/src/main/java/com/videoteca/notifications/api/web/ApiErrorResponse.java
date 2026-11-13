package com.videoteca.notifications.api.web;

import java.util.List;

/**
 * Cuerpo de error 400: {@code {"error": "Bad Request", "errors": [{"field": ..., "message": ...}]}}.
 */
public record ApiErrorResponse(String error, List<ApiErrorDetail> errors) {

}
