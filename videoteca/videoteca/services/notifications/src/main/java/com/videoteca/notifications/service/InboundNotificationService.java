package com.videoteca.notifications.service;

import com.videoteca.notifications.api.dto.InboundCreateRequest;
import com.videoteca.notifications.domain.InboundNotification;
import com.videoteca.notifications.domain.exception.NotificationNotFoundException;
import com.videoteca.notifications.domain.repository.InboundNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Recepción y gestión de notificaciones entrantes (persistencia en memoria).
 */
@Service
public class InboundNotificationService {

	private static final Logger log = LoggerFactory.getLogger(InboundNotificationService.class);

	private final InboundNotificationRepository repository;

	public InboundNotificationService(InboundNotificationRepository repository) {
		this.repository = repository;
	}

	/** Registra una notificación entrante como no leída ({@code read=false}). */
	public InboundNotification receive(InboundCreateRequest request) {
		InboundNotification record = new InboundNotification(
				UUID.randomUUID(),
				request.sender(),
				request.type(),
				request.subject(),
				request.body(),
				request.referenceId(),
				request.payload(),
				false,
				Instant.now());
		repository.save(record);
		log.info("Notificación recibida: id={}, type={}, sender={}", record.id(), record.type(), record.sender());
		return record;
	}

	public List<InboundNotification> list(Boolean read, String type, int limit) {
		return repository.findAll(read, type, limit);
	}

	public InboundNotification get(UUID id) {
		return repository.findById(id)
			.orElseThrow(() -> new NotificationNotFoundException("Notificación entrante no encontrada: " + id));
	}

	public InboundNotification markRead(UUID id, boolean read) {
		return repository.markRead(id, read)
			.orElseThrow(() -> new NotificationNotFoundException("Notificación entrante no encontrada: " + id));
	}

	public void delete(UUID id) {
		boolean deleted = repository.deleteById(id);
		if (!deleted) {
			throw new NotificationNotFoundException("Notificación entrante no encontrada: " + id);
		}
	}

}
