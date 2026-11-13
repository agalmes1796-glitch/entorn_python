package com.videoteca.notifications.api.dto;

/**
 * Cuerpo opcional de {@code PATCH /inbound/{id}/read}.
 *
 * @param read nuevo estado de lectura; si el body no se envía se usa el parámetro {@code ?read=}
 */
public record InboundReadRequest(
		Boolean read) {

}
