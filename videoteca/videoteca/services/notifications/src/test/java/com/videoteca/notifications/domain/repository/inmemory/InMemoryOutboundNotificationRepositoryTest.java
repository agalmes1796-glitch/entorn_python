package com.videoteca.notifications.domain.repository.inmemory;

import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de unidad de {@link InMemoryOutboundNotificationRepository} (sin Spring).
 */
class InMemoryOutboundNotificationRepositoryTest {

	private InMemoryOutboundNotificationRepository repository;

	@BeforeEach
	void setUp() {
		repository = new InMemoryOutboundNotificationRepository(10_000);
	}

	private OutboundNotification notification(String to, NotificationStatus status) {
		return notification(to, status, Instant.now());
	}

	private OutboundNotification notification(String to, NotificationStatus status, Instant createdAt) {
		return new OutboundNotification(UUID.randomUUID(), to, null, "subject", "body", "MOVIE_ADDED",
				"ref-1", null, status, createdAt, status == NotificationStatus.SENT ? Instant.now() : null, null);
	}

	@Test
	void saveAndFindById() {
		OutboundNotification record = notification("a@b.com", NotificationStatus.SENT);
		OutboundNotification saved = repository.save(record);

		assertThat(saved).isSameAs(record);
		Optional<OutboundNotification> found = repository.findById(record.id());
		assertThat(found).isPresent().hasValue(record);
	}

	@Test
	void findByIdReturnsEmptyForUnknownId() {
		assertThat(repository.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void saveReplacesExistingRecordBySameId() {
		// Simula un resend: mismo id, nuevo estado
		OutboundNotification failed = notification("a@b.com", NotificationStatus.FAILED).withFailure("smtp down");
		repository.save(failed);
		OutboundNotification sent = failed.withSentAt(Instant.now());
		repository.save(sent);

		assertThat(repository.findAll(null, 100)).hasSize(1);
		assertThat(repository.findById(failed.id()))
			.hasValueSatisfying(found -> assertThat(found.status()).isEqualTo(NotificationStatus.SENT));
	}

	@Test
	void findAllReturnsAllWhenNoStatusFilter() {
		repository.save(notification("a@b.com", NotificationStatus.SENT));
		repository.save(notification("c@d.com", NotificationStatus.FAILED));

		assertThat(repository.findAll(null, 100)).hasSize(2);
	}

	@Test
	void findAllFiltersByStatus() {
		repository.save(notification("sent@b.com", NotificationStatus.SENT));
		repository.save(notification("failed@d.com", NotificationStatus.FAILED));

		assertThat(repository.findAll(NotificationStatus.SENT, 100))
			.extracting(OutboundNotification::to)
			.containsExactly("sent@b.com");
		assertThat(repository.findAll(NotificationStatus.FAILED, 100))
			.extracting(OutboundNotification::to)
			.containsExactly("failed@d.com");
	}

	@Test
	void findAllReturnsEmptyListWhenStoreIsEmpty() {
		assertThat(repository.findAll(null, 100)).isEmpty();
		assertThat(repository.findAll(NotificationStatus.SENT, 100)).isEmpty();
	}

	@Test
	void findAllAppliesLimit() {
		for (int i = 0; i < 5; i++) {
			repository.save(notification("user" + i + "@b.com", NotificationStatus.SENT));
		}

		assertThat(repository.findAll(null, 3)).hasSize(3);
	}

	@Test
	void findAllOrdersNewestFirst() {
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		repository.save(notification("old@b.com", NotificationStatus.SENT, base));
		repository.save(notification("new@b.com", NotificationStatus.SENT, base.plusSeconds(3600)));
		repository.save(notification("mid@b.com", NotificationStatus.SENT, base.plusSeconds(1800)));

		assertThat(repository.findAll(null, 10))
			.extracting(OutboundNotification::to)
			.containsExactly("new@b.com", "mid@b.com", "old@b.com");
	}

	@Test
	void deleteByIdRemovesExistingRecord() {
		OutboundNotification record = notification("a@b.com", NotificationStatus.SENT);
		repository.save(record);

		assertThat(repository.deleteById(record.id())).isTrue();
		assertThat(repository.findById(record.id())).isEmpty();
	}

	@Test
	void deleteByIdReturnsFalseForUnknownId() {
		assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
	}

	@Test
	void findAllReturnsImmutableList() {
		repository.save(notification("a@b.com", NotificationStatus.SENT));
		List<OutboundNotification> result = repository.findAll(null, 10);

		assertThat(result).isUnmodifiable();
	}

	@Test
	void saveEvictsOldestEntryWhenStoreIsFull() {
		repository = new InMemoryOutboundNotificationRepository(3);
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		OutboundNotification oldest = notification("old@b.com", NotificationStatus.SENT, base);
		OutboundNotification middle = notification("mid@b.com", NotificationStatus.SENT, base.plusSeconds(60));
		OutboundNotification newest = notification("new@b.com", NotificationStatus.SENT, base.plusSeconds(120));
		repository.save(oldest);
		repository.save(middle);
		repository.save(newest);

		// Store lleno: una entrada nueva evicta la más antigua
		OutboundNotification extra = notification("extra@b.com", NotificationStatus.SENT, base.plusSeconds(180));
		repository.save(extra);

		assertThat(repository.findById(oldest.id())).isEmpty();
		assertThat(repository.findById(middle.id())).isPresent();
		assertThat(repository.findById(newest.id())).isPresent();
		assertThat(repository.findById(extra.id())).isPresent();
		assertThat(repository.findAll(null, 10)).hasSize(3);
	}

	@Test
	void saveDoesNotEvictWhenUpdatingAnExistingRecord() {
		repository = new InMemoryOutboundNotificationRepository(2);
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		OutboundNotification a = notification("a@b.com", NotificationStatus.FAILED, base).withFailure("smtp down");
		OutboundNotification b = notification("c@d.com", NotificationStatus.SENT, base.plusSeconds(60));
		repository.save(a);
		repository.save(b);

		// Actualización (resend) de un id ya presente no debe evictar nada
		repository.save(a.withSentAt(base.plusSeconds(120)));

		assertThat(repository.findById(a.id()))
			.hasValueSatisfying(found -> assertThat(found.status()).isEqualTo(NotificationStatus.SENT));
		assertThat(repository.findById(b.id())).isPresent();
		assertThat(repository.findAll(null, 10)).hasSize(2);
	}

}
