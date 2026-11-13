package com.videoteca.notifications.api;

import com.videoteca.notifications.api.dto.InboundCreateRequest;
import com.videoteca.notifications.api.dto.InboundReadRequest;
import com.videoteca.notifications.domain.InboundNotification;
import com.videoteca.notifications.domain.exception.BadRequestException;
import com.videoteca.notifications.service.InboundNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Endpoints de notificaciones entrantes (recepción de otros servicios).
 */
@RestController
@RequestMapping("/api/v1/notifications/inbound")
public class InboundNotificationController {

	private final InboundNotificationService service;

	public InboundNotificationController(InboundNotificationService service) {
		this.service = service;
	}

	/** Registra una notificación entrante. 201 con el registro completo ({@code read=false}). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InboundNotification create(@Valid @RequestBody InboundCreateRequest request) {
		return service.receive(request);
	}

	/** Listado, de más reciente a más antigua, con filtros opcionales. */
	@GetMapping
	public List<InboundNotification> list(@RequestParam(required = false) Boolean read,
			@RequestParam(required = false) String type,
			@RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit) {
		return service.list(read, type, limit);
	}

	/** Detalle. 404 JSON si no existe. */
	@GetMapping("/{id}")
	public InboundNotification detail(@PathVariable UUID id) {
		return service.get(id);
	}

	/**
	 * Marca como leída/no leída. Acepta el estado en el body
	 * ({@code {"read": true|false}}) o en el parámetro {@code ?read=}.
	 * 200 con el registro actualizado; 404 si no existe.
	 */
	@PatchMapping("/{id}/read")
	public InboundNotification markRead(@PathVariable UUID id,
			@RequestBody(required = false) InboundReadRequest body,
			@RequestParam(required = false) Boolean read) {
		boolean value;
		if (body != null && body.read() != null) {
			value = body.read();
		}
		else if (read != null) {
			value = read;
		}
		else {
			throw new BadRequestException(
					"Indica el estado de lectura: body { \"read\": true|false } o parámetro ?read=true|false");
		}
		return service.markRead(id, value);
	}

	/** Elimina la notificación. 204 si existe; 404 si no. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		service.delete(id);
	}

}
