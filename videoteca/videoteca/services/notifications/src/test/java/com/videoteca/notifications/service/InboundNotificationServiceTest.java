package com.videoteca.notifications.service;

import com.videoteca.notifications.api.dto.InboundCreateRequest;
import com.videoteca.notifications.domain.InboundNotification;
import com.videoteca.notifications.domain.exception.NotificationNotFoundException;
import com.videoteca.notifications.domain.repository.InboundNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de unidad de {@link InboundNotificationService} con mocks (sin Spring).
 */
@ExtendWith(MockitoExtension.class)
class InboundNotificationServiceTest {

	@Mock
	private InboundNotificationRepository repository;

	private InboundNotificationService service;

	@BeforeEach
	void setUp() {
		service = new InboundNotificationService(repository);
	}

	private InboundCreateRequest request() {
		return new InboundCreateRequest("movies", "MOVIE_REMOVED", "Película retirada",
				"La película 42 fue retirada del catálogo.", "42", Map.of("movieId", 42));
	}

	@Test
	void receiveSavesUnreadNotificationWithTimestamp() {
		InboundNotification result = service.receive(request());

		assertThat(result.id()).isNotNull();
		assertThat(result.read()).isFalse();
		assertThat(result.receivedAt()).isNotNull();
		assertThat(result.sender()).isEqualTo("movies");
		assertThat(result.type()).isEqualTo("MOVIE_REMOVED");
		assertThat(result.referenceId()).isEqualTo("42");
		verify(repository).save(result);
	}

	@Test
	void listDelegatesToRepositoryWithFilters() {
		when(repository.findAll(false, "MOVIE_REMOVED", 50)).thenReturn(List.of());

		assertThat(service.list(false, "MOVIE_REMOVED", 50)).isEmpty();
		verify(repository).findAll(false, "MOVIE_REMOVED", 50);
	}

	@Test
	void getReturnsNotificationWhenItExists() {
		InboundNotification record = new InboundNotification(UUID.randomUUID(), "svc", "T", "s", "b",
				null, null, false, java.time.Instant.now());
		when(repository.findById(record.id())).thenReturn(Optional.of(record));

		assertThat(service.get(record.id())).isSameAs(record);
	}

	@Test
	void getThrowsNotFoundWhenIdDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(id))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	void markReadDelegatesToRepository() {
		InboundNotification record = new InboundNotification(UUID.randomUUID(), "svc", "T", "s", "b",
				null, null, false, java.time.Instant.now());
		InboundNotification updated = record.withRead(true);
		when(repository.markRead(record.id(), true)).thenReturn(Optional.of(updated));

		assertThat(service.markRead(record.id(), true)).isSameAs(updated);
	}

	@Test
	void markReadThrowsNotFoundWhenIdDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.markRead(id, true)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.markRead(id, true))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	void deleteRemovesWhenRecordExists() {
		UUID id = UUID.randomUUID();
		when(repository.deleteById(id)).thenReturn(true);

		service.delete(id);

		verify(repository).deleteById(id);
	}

	@Test
	void deleteThrowsNotFoundWhenIdDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.deleteById(id)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(id))
			.isInstanceOf(NotificationNotFoundException.class);
	}

}
