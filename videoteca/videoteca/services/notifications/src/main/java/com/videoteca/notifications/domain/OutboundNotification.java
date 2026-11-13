package com.videoteca.notifications.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Notificación SALIENTE: un correo enviado (o cuyo envío se ha intentado) a un destinatario.
 *
 * <p>Es inmutable: los cambios de estado devuelven una nueva instancia
 * ({@link #withSentAt(Instant)} / {@link #withFailure(String)}).</p>
 *
 * @param id           identificador único (UUID)
 * @param to           destinatario principal
 * @param cc           destinatarios en copia (opcional)
 * @param subject      asunto del correo
 * @param body         cuerpo del correo
 * @param type         tipo de notificación (p. ej. MOVIE_ADDED); genérico para cualquier dominio
 * @param referenceId  referencia al recurso de origen (opcional)
 * @param payload      datos extra en JSON (opcional)
 * @param status       estado actual (SENT o FAILED)
 * @param createdAt    instante de creación
 * @param sentAt       instante de envío (solo si status=SENT)
 * @param errorMessage mensaje de error sanitizado (solo si status=FAILED;
 *                     nunca el detalle crudo de JavaMail)
 */
public record OutboundNotification(
		UUID id,
		String to,
		List<String> cc,
		String subject,
		String body,
		String type,
		String referenceId,
		Map<String, Object> payload,
		NotificationStatus status,
		Instant createdAt,
		Instant sentAt,
		String errorMessage) {

	public OutboundNotification {
		Objects.requireNonNull(id, "id no puede ser nulo");
		Objects.requireNonNull(to, "to no puede ser nulo");
		Objects.requireNonNull(subject, "subject no puede ser nulo");
		Objects.requireNonNull(body, "body no puede ser nulo");
		Objects.requireNonNull(type, "type no puede ser nulo");
		Objects.requireNonNull(status, "status no puede ser nulo");
		Objects.requireNonNull(createdAt, "createdAt no puede ser nulo");
		if (cc != null) {
			cc = List.copyOf(cc);
		}
		if (payload != null) {
			payload = Map.copyOf(payload);
		}
	}

	/** Devuelve una copia marcada como enviada (SENT) en el instante indicado. */
	public OutboundNotification withSentAt(Instant sentAt) {
		Objects.requireNonNull(sentAt, "sentAt no puede ser nulo");
		return new OutboundNotification(id, to, cc, subject, body, type, referenceId, payload,
				NotificationStatus.SENT, createdAt, sentAt, null);
	}

	/** Devuelve una copia marcada como fallida (FAILED) con el mensaje de error. */
	public OutboundNotification withFailure(String failureMessage) {
		Objects.requireNonNull(failureMessage, "errorMessage no puede ser nulo");
		return new OutboundNotification(id, to, cc, subject, body, type, referenceId, payload,
				NotificationStatus.FAILED, createdAt, null, failureMessage);
	}

}
