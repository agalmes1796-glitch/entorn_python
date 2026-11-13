package com.videoteca.notifications.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Petición para el envío de una notificación saliente.
 *
 * @param to          destinatario principal (obligatorio, email válido)
 * @param cc          destinatarios en copia (opcional, emails válidos, máx. 20)
 * @param subject     asunto (obligatorio, máx. 200 caracteres)
 * @param body        cuerpo (obligatorio, máx. 20 000 caracteres)
 * @param type        tipo de notificación, p. ej. MOVIE_ADDED (obligatorio,
 *                    {@code [A-Z0-9_]{2,50}})
 * @param referenceId referencia al recurso de origen (opcional, máx. 100)
 * @param payload     datos extra en JSON (opcional)
 */
public record OutboundSendRequest(
		@NotBlank @Email String to,
		@Size(max = 20) List<@Email String> cc,
		@NotBlank @Size(max = 200) String subject,
		@NotBlank @Size(max = 20_000) String body,
		@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,50}") String type,
		@Size(max = 100) String referenceId,
		Map<String, Object> payload) {

}
