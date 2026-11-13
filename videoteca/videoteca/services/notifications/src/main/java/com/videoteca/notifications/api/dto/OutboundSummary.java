package com.videoteca.notifications.api.dto;

import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumen de una notificación saliente para listados.
 */
public record OutboundSummary(
		UUID id,
		String to,
		String subject,
		String type,
		NotificationStatus status,
		Instant createdAt,
		Instant sentAt) {

	public static OutboundSummary from(OutboundNotification notification) {
		return new OutboundSummary(notification.id(), notification.to(), notification.subject(),
				notification.type(), notification.status(), notification.createdAt(), notification.sentAt());
	}

}
