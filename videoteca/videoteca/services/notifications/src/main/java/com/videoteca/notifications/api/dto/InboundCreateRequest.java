package com.videoteca.notifications.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Petición para registrar una notificación entrante.
 *
 * @param sender       servicio/origen (obligatorio, {@code [A-Za-z0-9._-]{1,100}})
 * @param type         tipo de notificación, p. ej. MOVIE_REMOVED (obligatorio,
 *                     {@code [A-Z0-9_]{2,50}})
 * @param subject      asunto (obligatorio, máx. 200 caracteres)
 * @param body         contenido (obligatorio, máx. 20 000 caracteres)
 * @param referenceId  referencia al recurso de origen (opcional, máx. 100)
 * @param payload      datos extra en JSON (opcional)
 */
public record InboundCreateRequest(
		@NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,100}") String sender,
		@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,50}") String type,
		@NotBlank @Size(max = 200) String subject,
		@NotBlank @Size(max = 20_000) String body,
		@Size(max = 100) String referenceId,
		Map<String, Object> payload) {

}
