package com.videoteca.notifications.api.web;

/**
 * Detalle de un error de validación: campo y mensaje.
 */
public record ApiErrorDetail(String field, String message) {

}
