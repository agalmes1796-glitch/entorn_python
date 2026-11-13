package com.videoteca.notifications.domain;

/**
 * Estado de una notificación saliente.
 */
public enum NotificationStatus {

	/** El correo se envió correctamente por SMTP. */
	SENT,

	/** El envío falló (p. ej., sin servidor SMTP disponible) y quedó registrado el error. */
	FAILED

}
