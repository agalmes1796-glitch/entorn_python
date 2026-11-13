package com.videoteca.notifications.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Notificación ENTRANTE: una notificación recibida de un servicio externo
 * (p. ej., movies avisando de una MOVIE_REMOVED).
 *
 * <p>Es inmutable: {@link #withRead(boolean)} devuelve una copia con el flag actualizado.</p>
 *
 * @param id           identificador único (UUID)
 * @param sender       servicio/origen de la notificación
 * @param type         tipo de notificación (p. ej. MOVIE_REMOVED)
 * @param subject      asunto/suma de la notificación
 * @param body         contenido
 * @param referenceId  referencia al recurso de origen (opcional)
 * @param payload      datos extra en JSON (opcional)
 * @param read         si ha sido leída (false por defecto)
 * @param receivedAt   instante de recepción
 */
public record InboundNotification(
		UUID id,
		String sender,
		String type,
		String subject,
		String body,
		String referenceId,
		Map<String, Object> payload,
		boolean read,
		Instant receivedAt) {

	public InboundNotification {
		Objects.requireNonNull(id, "id no puede ser nulo");
		Objects.requireNonNull(sender, "sender no puede ser nulo");
		Objects.requireNonNull(type, "type no puede ser nulo");
		Objects.requireNonNull(subject, "subject no puede ser nulo");
		Objects.requireNonNull(body, "body no puede ser nulo");
		Objects.requireNonNull(receivedAt, "receivedAt no puede ser nulo");
		if (payload != null) {
			payload = Map.copyOf(payload);
		}
	}

	/** Devuelve una copia con el estado de lectura actualizado. */
	public InboundNotification withRead(boolean value) {
		return new InboundNotification(id, sender, type, subject, body, referenceId, payload, value, receivedAt);
	}

}
