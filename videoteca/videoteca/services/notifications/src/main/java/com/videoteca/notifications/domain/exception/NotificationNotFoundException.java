package com.videoteca.notifications.domain.exception;

/**
 * Excepción de dominio para notificaciones inexistentes.
 * Se traduce en una respuesta 404 JSON.
 */
public class NotificationNotFoundException extends RuntimeException {

	public NotificationNotFoundException(String message) {
		super(message);
	}

}
