package com.videoteca.notifications.domain.exception;

/**
 * Conflicto en la operación solicitada: por ejemplo, reenviar una
 * notificación que ya está en estado {@code SENT}, o un reenvío
 * concurrente ya en curso. La traduce {@code GlobalExceptionHandler}
 * a HTTP 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
