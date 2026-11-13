package com.videoteca.notifications.api.dto;

import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code POST /outbound/send}: resumen del estado del envío.
 */
public record OutboundSendResponse(
		UUID id,
		NotificationStatus status,
		String to,
		String type,
		Instant sentAt,
		String errorMessage) {

	public static OutboundSendResponse from(OutboundNotification notification) {
		return new OutboundSendResponse(notification.id(), notification.status(), notification.to(),
				notification.type(), notification.sentAt(), notification.errorMessage());
	}

}
