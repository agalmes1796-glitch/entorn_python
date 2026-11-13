package com.videoteca.notifications.api.web;

/**
 * Cuerpo de error 500 genérico de seguridad: mensaje fijo, sin stacktrace ni
 * detalles internos en el JSON.
 */
public record InternalErrorResponse(String error, String message) {

}
