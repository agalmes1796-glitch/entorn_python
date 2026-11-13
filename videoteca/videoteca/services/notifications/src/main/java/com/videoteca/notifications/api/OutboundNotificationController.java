package com.videoteca.notifications.api;

import com.videoteca.notifications.api.dto.OutboundSendRequest;
import com.videoteca.notifications.api.dto.OutboundSendResponse;
import com.videoteca.notifications.api.dto.OutboundSummary;
import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;
import com.videoteca.notifications.service.OutboundNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de notificaciones salientes (envío por email).
 */
@RestController
@RequestMapping("/api/v1/notifications/outbound")
public class OutboundNotificationController {

	private final OutboundNotificationService service;

	public OutboundNotificationController(OutboundNotificationService service) {
		this.service = service;
	}

	/**
	 * Envía una notificación por email.
	 *
	 * <p>Responde <b>202</b> siempre que la petición sea válida: incluso si el
	 * envío SMTP falla (p. ej., sin servidor en dev), el registro queda
	 * persistido con {@code status=FAILED} y su {@code errorMessage}.</p>
	 */
	@PostMapping("/send")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public OutboundSendResponse send(@Valid @RequestBody OutboundSendRequest request) {
		return OutboundSendResponse.from(service.send(request));
	}

	/** Listado de resúmenes, de más reciente a más antigua. */
	@GetMapping
	public List<OutboundSummary> list(@RequestParam(required = false) NotificationStatus status,
			@RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit) {
		return service.list(status, limit).stream()
			.map(OutboundSummary::from)
			.toList();
	}

	/** Detalle completo de una notificación. 404 JSON si no existe. */
	@GetMapping("/{id}")
	public OutboundNotification detail(@PathVariable UUID id) {
		return service.get(id);
	}

	/** Reintenta el envío. 200 con el estado nuevo, 404 si no existe. */
	@PostMapping("/{id}/resend")
	public OutboundNotification resend(@PathVariable UUID id) {
		return service.resend(id);
	}

}
