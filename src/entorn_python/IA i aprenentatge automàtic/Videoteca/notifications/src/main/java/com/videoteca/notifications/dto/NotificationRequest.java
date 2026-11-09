package com.videoteca.notifications.dto;

import com.videoteca.notifications.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petició d'alta/actualització d'una notificació.
 *
 * <p>El camp {@code type} és opcional: el constructor compacte
 * normalitza el valor a {@link NotificationType#INFO} quan arriba null
 * (p. ex., quan el JSON no porta el camp).</p>
 */
public record NotificationRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String message,

        NotificationType type
) {

    /** Constructor compacte: aplica el valor per defecte al tipus. */
    public NotificationRequest {
        if (type == null) {
            type = NotificationType.INFO;
        }
    }
}
