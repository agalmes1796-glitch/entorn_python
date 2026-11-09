package com.videoteca.notifications.dto;

import com.videoteca.notifications.model.Notification;
import com.videoteca.notifications.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de l'API: projecció immutable de l'entitat {@link Notification}.
 */
public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt,
        Instant updatedAt
) {

    /** Crea la resposta a partir de l'entitat persistida. */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }
}
