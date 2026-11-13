package com.videoteca.notifications.service;

import com.videoteca.notifications.api.dto.OutboundSendRequest;
import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;
import com.videoteca.notifications.domain.exception.ConflictException;
import com.videoteca.notifications.domain.exception.NotificationNotFoundException;
import com.videoteca.notifications.domain.repository.OutboundNotificationRepository;
import com.videoteca.notifications.util.EmailMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Envío de notificaciones salientes por email (SMTP) con registro en memoria.
 *
 * <p>Un fallo de SMTP <b>NO</b> propaga excepción: la notificación se registra
 * igualmente con {@code status=FAILED} y un {@code errorMessage}
 * <b>sanitizado</b> (nunca el detalle crudo de JavaMail), de modo que la API
 * siempre responde 202 (el registro existe). El detalle completo queda solo
 * en los logs.</p>
 */
@Service
public class OutboundNotificationService {

	private static final Logger log = LoggerFactory.getLogger(OutboundNotificationService.class);

	private static final String MAIL_AUTH_FAILURE = "Fallo de autenticación SMTP";

	private static final String MAIL_SEND_FAILURE = "Fallo de envío SMTP";

	private static final String GENERIC_FAILURE = "Error interno al enviar la notificación";

	private final OutboundNotificationRepository repository;

	private final JavaMailSender mailSender;

	/**
	 * Guarda de carrera: ids con un envío/reenvío en curso. Evita doble
	 * entrega concurrente del mismo reenvío.
	 */
	private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

	public OutboundNotificationService(OutboundNotificationRepository repository, JavaMailSender mailSender) {
		this.repository = repository;
		this.mailSender = mailSender;
	}

	/**
	 * Crea, intenta enviar y persiste la notificación.
	 * Nunca lanza por fallos de SMTP.
	 */
	public OutboundNotification send(OutboundSendRequest request) {
		OutboundNotification pending = new OutboundNotification(
				UUID.randomUUID(),
				request.to(),
				request.cc(),
				request.subject(),
				request.body(),
				request.type(),
				request.referenceId(),
				request.payload(),
				NotificationStatus.FAILED, // valor provisional; se resuelve con el resultado del envío
				Instant.now(),
				null,
				null);
		OutboundNotification result = attemptDelivery(pending);
		return repository.save(result);
	}

	/**
	 * Reintenta el envío de una notificación existente (por ejemplo, una FAILED
	 * tras recuperar el servidor SMTP).
	 *
	 * @throws ConflictException si la notificación ya está en {@code SENT}
	 *                           o si ya hay otro reenvío en curso para ese id.
	 */
	public OutboundNotification resend(UUID id) {
		boolean added = inFlight.add(id);
		try {
			if (!added) {
				throw new ConflictException("La notificación " + id + ": ya hay un reenvío en curso");
			}
			OutboundNotification existing = repository.findById(id)
				.orElseThrow(() -> new NotificationNotFoundException("Notificación saliente no encontrada: " + id));
			if (existing.status() == NotificationStatus.SENT) {
				throw new ConflictException("La notificación ya fue enviada: " + id);
			}
			OutboundNotification result = attemptDelivery(existing);
			return repository.save(result);
		}
		finally {
			// Solo quita quien añadió: un reenvío rechazado (ya en curso)
			// no debe liberar la entrada del reenvío que sí está en vuelo.
			if (added) {
				inFlight.remove(id);
			}
		}
	}

	public List<OutboundNotification> list(NotificationStatus status, int limit) {
		return repository.findAll(status, limit);
	}

	public OutboundNotification get(UUID id) {
		return repository.findById(id)
			.orElseThrow(() -> new NotificationNotFoundException("Notificación saliente no encontrada: " + id));
	}

	private OutboundNotification attemptDelivery(OutboundNotification notification) {
		try {
			mailSender.send(toMailMessage(notification));
			Instant sentAt = Instant.now();
			log.info("Notificación enviada: id={}, type={}, to={}, cc={}", notification.id(), notification.type(),
					EmailMasker.mask(notification.to()), maskedCc(notification.cc()));
			return notification.withSentAt(sentAt);
		}
		catch (Exception ex) {
			// El detalle completo (host:puerto, stacktrace) solo va a los logs;
			// lo que se persiste/expone es un mensaje sanitizado.
			log.warn("No se pudo enviar la notificación: id={}, type={}, to={}, cc={}", notification.id(),
					notification.type(), EmailMasker.mask(notification.to()), maskedCc(notification.cc()), ex);
			return notification.withFailure(sanitizeError(ex));
		}
	}

	/**
	 * Mensaje de error sanitizado para persistir/exponer: nunca el texto
	 * crudo de JavaMail (host:puerto, respuestas del servidor SMTP).
	 */
	static String sanitizeError(Exception ex) {
		if (ex instanceof MailAuthenticationException) {
			return MAIL_AUTH_FAILURE;
		}
		if (ex instanceof MailException) {
			return MAIL_SEND_FAILURE;
		}
		return GENERIC_FAILURE;
	}

	/** Lista de cc enmascarada para logs (p. ej. {@code [c***@dominio.com]}). */
	private static String maskedCc(List<String> cc) {
		if (cc == null || cc.isEmpty()) {
			return "[]";
		}
		return cc.stream().map(EmailMasker::mask).toList().toString();
	}

	private SimpleMailMessage toMailMessage(OutboundNotification notification) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(notification.to());
		if (notification.cc() != null && !notification.cc().isEmpty()) {
			message.setCc(notification.cc().toArray(new String[0]));
		}
		message.setSubject(notification.subject());
		message.setText(notification.body());
		return message;
	}

}
