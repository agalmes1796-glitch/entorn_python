package com.videoteca.notifications.service;

import com.videoteca.notifications.api.dto.OutboundSendRequest;
import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;
import com.videoteca.notifications.domain.exception.ConflictException;
import com.videoteca.notifications.domain.exception.NotificationNotFoundException;
import com.videoteca.notifications.domain.repository.OutboundNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de unidad de {@link OutboundNotificationService} con mocks (sin Spring, sin SMTP).
 */
@ExtendWith(MockitoExtension.class)
class OutboundNotificationServiceTest {

	@Mock
	private OutboundNotificationRepository repository;

	@Mock
	private JavaMailSender mailSender;

	private OutboundNotificationService service;

	@BeforeEach
	void setUp() {
		service = new OutboundNotificationService(repository, mailSender);
		// save() devuelve el registro recibido (como hace la implementación real)
		lenient().when(repository.save(any(OutboundNotification.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private OutboundSendRequest request() {
		return new OutboundSendRequest("usuario@dominio.com", List.of("cc@dominio.com"), "Película añadida",
				"La película Blade Runner fue añadida.", "MOVIE_ADDED", "123", Map.of("movieId", 42));
	}

	@Test
	void sendSavesNotificationAsSentWhenMailSucceeds() {
		OutboundNotification result = service.send(request());

		assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
		assertThat(result.sentAt()).isNotNull();
		assertThat(result.errorMessage()).isNull();
		assertThat(result.to()).isEqualTo("usuario@dominio.com");
		assertThat(result.createdAt()).isNotNull();
		verify(mailSender).send(any(SimpleMailMessage.class));
		verify(repository).save(result);
	}

	@Test
	void sendSavesNotificationAsFailedWhenMailThrows() {
		doThrow(new MailSendException("Connection refused: localhost/127.0.0.1:25"))
			.when(mailSender).send(any(SimpleMailMessage.class));

		OutboundNotification result = service.send(request());

		assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
		// Sanitizado: nunca el detalle crudo de JavaMail (host:puerto)
		assertThat(result.errorMessage()).isEqualTo("Fallo de envío SMTP");
		assertThat(result.sentAt()).isNull();
		verify(repository).save(result);
	}

	@Test
	void sendSavesAuthenticationFailureMessageWhenMailAuthenticationFails() {
		doThrow(new MailAuthenticationException("535 Authentication failed: user=admin@dominio.com"))
			.when(mailSender).send(any(SimpleMailMessage.class));

		OutboundNotification result = service.send(request());

		assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(result.errorMessage()).isEqualTo("Fallo de autenticación SMTP");
	}

	@Test
	void sendDoesNotPropagateExceptionWhenMailFails() {
		doThrow(new RuntimeException("boom")).when(mailSender).send(any(SimpleMailMessage.class));

		OutboundNotification result = service.send(request());

		assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
		// Excepción no-SMTP: mensaje genérico, sin el texto de la excepción
		assertThat(result.errorMessage()).isEqualTo("Error interno al enviar la notificación");
	}

	@Test
	void sendMapsAllFieldsToTheMailMessage() {
		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		service.send(request());

		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();
		assertThat(message.getTo()).containsExactly("usuario@dominio.com");
		assertThat(message.getCc()).containsExactly("cc@dominio.com");
		assertThat(message.getSubject()).isEqualTo("Película añadida");
		assertThat(message.getText()).isEqualTo("La película Blade Runner fue añadida.");
	}

	@Test
	void sendDoesNotSetCcWhenItIsEmpty() {
		OutboundSendRequest withoutCc = new OutboundSendRequest("usuario@dominio.com", null, "s", "b",
				"MOVIE_ADDED", null, null);
		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		service.send(withoutCc);

		verify(mailSender).send(captor.capture());
		assertThat(captor.getValue().getCc()).isNull();
	}

	@Test
	void resendSucceedsWhenMailRecovers() {
		UUID id = UUID.randomUUID();
		OutboundNotification failed = new OutboundNotification(id, "a@b.com", null, "s", "b", "MOVIE_ADDED",
				"1", null, NotificationStatus.FAILED, Instant.now(), null, "smtp down");
		when(repository.findById(id)).thenReturn(Optional.of(failed));

		OutboundNotification result = service.resend(id);

		assertThat(result.id()).isEqualTo(id);
		assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
		assertThat(result.sentAt()).isNotNull();
		assertThat(result.errorMessage()).isNull();
		assertThat(result.createdAt()).isEqualTo(failed.createdAt());
		verify(repository).save(result);
	}

	@Test
	void resendSavesFailedAgainWhenMailStillThrows() {
		UUID id = UUID.randomUUID();
		OutboundNotification failed = new OutboundNotification(id, "a@b.com", null, "s", "b", "MOVIE_ADDED",
				"1", null, NotificationStatus.FAILED, Instant.now(), null, "smtp down");
		when(repository.findById(id)).thenReturn(Optional.of(failed));
		doThrow(new MailSendException("still down")).when(mailSender).send(any(SimpleMailMessage.class));

		OutboundNotification result = service.resend(id);

		assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(result.errorMessage()).isEqualTo("Fallo de envío SMTP");
		verify(repository).save(result);
	}

	@Test
	void resendThrowsConflictWhenNotificationAlreadySent() {
		UUID id = UUID.randomUUID();
		OutboundNotification sent = new OutboundNotification(id, "a@b.com", null, "s", "b", "MOVIE_ADDED",
				"1", null, NotificationStatus.SENT, Instant.now(), Instant.now(), null);
		when(repository.findById(id)).thenReturn(Optional.of(sent));

		assertThatThrownBy(() -> service.resend(id))
			.isInstanceOf(ConflictException.class)
			.hasMessage("La notificación ya fue enviada: " + id);
	}

	@Test
	void resendThrowsConflictWhenAnotherResendIsInFlight() throws Exception {
		UUID id = UUID.randomUUID();
		OutboundNotification failed = new OutboundNotification(id, "a@b.com", null, "s", "b", "MOVIE_ADDED",
				"1", null, NotificationStatus.FAILED, Instant.now(), null, "smtp down");
		when(repository.findById(id)).thenReturn(Optional.of(failed));

		// El primer reenvío "queda atascado" en el envío SMTP
		CountDownLatch deliveryStarted = new CountDownLatch(1);
		CountDownLatch releaseDelivery = new CountDownLatch(1);
		doAnswer(invocation -> {
			deliveryStarted.countDown();
			releaseDelivery.await();
			return null;
		}).when(mailSender).send(any(SimpleMailMessage.class));

		AtomicReference<Throwable> firstResendError = new AtomicReference<>();
		Thread firstResend = new Thread(() -> {
			try {
				service.resend(id);
			}
			catch (Throwable ex) {
				firstResendError.set(ex);
			}
		});
		firstResend.start();

		try {
			assertThat(deliveryStarted.await(5, TimeUnit.SECONDS)).isTrue();
			// Segundo reenvío concurrente para el mismo id → 409 (conflicto de carrera)
			assertThatThrownBy(() -> service.resend(id))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("ya hay un reenvío en curso");
		}
		finally {
			releaseDelivery.countDown();
		}

		firstResend.join(5_000);
		assertThat(firstResendError.get())
			.as("El primer reenvío debería haber completado sin error").isNull();
		verify(repository).save(any(OutboundNotification.class));
	}

	@Test
	void resendThrowsNotFoundWhenIdDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resend(id))
			.isInstanceOf(NotificationNotFoundException.class)
			.hasMessageContaining(id.toString());
	}

	@Test
	void getThrowsNotFoundWhenIdDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(id))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	void listDelegatesToRepositoryWithFilters() {
		when(repository.findAll(NotificationStatus.SENT, 10)).thenReturn(List.of());

		assertThat(service.list(NotificationStatus.SENT, 10)).isEmpty();
		verify(repository).findAll(NotificationStatus.SENT, 10);
	}

}
