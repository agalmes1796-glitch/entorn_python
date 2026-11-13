package com.videoteca.notifications.domain.exception;

/**
 * Excepción de dominio para peticiones mal formadas (p. ej., falta de parámetro
 * obligatorio no cubierto por Bean Validation). Se traduce en una respuesta 400 JSON.
 */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}
