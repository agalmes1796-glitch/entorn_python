package com.videoteca.notifications.domain.repository.inmemory;

import com.videoteca.notifications.domain.InboundNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de unidad de {@link InMemoryInboundNotificationRepository} (sin Spring).
 */
class InMemoryInboundNotificationRepositoryTest {

	private InMemoryInboundNotificationRepository repository;

	@BeforeEach
	void setUp() {
		repository = new InMemoryInboundNotificationRepository(10_000);
	}

	private InboundNotification notification(String sender, String type, boolean read) {
		return notification(sender, type, read, Instant.now());
	}

	private InboundNotification notification(String sender, String type, boolean read, Instant receivedAt) {
		return new InboundNotification(UUID.randomUUID(), sender, type, "subject", "body", "ref-1", null,
				read, receivedAt);
	}

	@Test
	void saveAndFindById() {
		InboundNotification record = notification("movies", "MOVIE_REMOVED", false);
		InboundNotification saved = repository.save(record);

		assertThat(saved).isSameAs(record);
		Optional<InboundNotification> found = repository.findById(record.id());
		assertThat(found).isPresent().hasValue(record);
	}

	@Test
	void findByIdReturnsEmptyForUnknownId() {
		assertThat(repository.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void findAllReturnsAllWhenNoFilters() {
		repository.save(notification("svc-a", "TYPE_A", false));
		repository.save(notification("svc-b", "TYPE_B", true));

		assertThat(repository.findAll(null, null, 100)).hasSize(2);
	}

	@Test
	void findAllFiltersByReadTrue() {
		repository.save(notification("svc-a", "TYPE_A", true));
		repository.save(notification("svc-b", "TYPE_B", false));

		assertThat(repository.findAll(true, null, 100))
			.extracting(InboundNotification::sender)
			.containsExactly("svc-a");
	}

	@Test
	void findAllFiltersByReadFalse() {
		repository.save(notification("svc-a", "TYPE_A", true));
		repository.save(notification("svc-b", "TYPE_B", false));

		assertThat(repository.findAll(false, null, 100))
			.extracting(InboundNotification::sender)
			.containsExactly("svc-b");
	}

	@Test
	void findAllFiltersByType() {
		repository.save(notification("svc-a", "MOVIE_REMOVED", false));
		repository.save(notification("svc-b", "MOVIE_ADDED", false));

		assertThat(repository.findAll(null, "MOVIE_REMOVED", 100))
			.extracting(InboundNotification::sender)
			.containsExactly("svc-a");
	}

	@Test
	void findAllCombinesReadAndTypeFilters() {
		repository.save(notification("svc-a", "TYPE_A", true));
		repository.save(notification("svc-b", "TYPE_A", false));
		repository.save(notification("svc-c", "TYPE_B", true));

		assertThat(repository.findAll(true, "TYPE_A", 100))
			.extracting(InboundNotification::sender)
			.containsExactly("svc-a");
	}

	@Test
	void findAllReturnsEmptyListWhenStoreIsEmpty() {
		assertThat(repository.findAll(null, null, 100)).isEmpty();
		assertThat(repository.findAll(true, "TYPE_A", 100)).isEmpty();
	}

	@Test
	void findAllAppliesLimit() {
		for (int i = 0; i < 5; i++) {
			repository.save(notification("svc-" + i, "TYPE_A", false));
		}

		assertThat(repository.findAll(null, null, 2)).hasSize(2);
	}

	@Test
	void findAllOrdersNewestFirst() {
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		repository.save(notification("old-svc", "TYPE_A", false, base));
		repository.save(notification("new-svc", "TYPE_A", false, base.plusSeconds(3600)));
		repository.save(notification("mid-svc", "TYPE_A", false, base.plusSeconds(1800)));

		assertThat(repository.findAll(null, null, 10))
			.extracting(InboundNotification::sender)
			.containsExactly("new-svc", "mid-svc", "old-svc");
	}

	@Test
	void markReadTrueReturnsUpdatedRecord() {
		InboundNotification record = notification("svc-a", "TYPE_A", false);
		repository.save(record);

		Optional<InboundNotification> updated = repository.markRead(record.id(), true);

		assertThat(updated).isPresent();
		assertThat(updated.get().read()).isTrue();
		assertThat(updated.get().id()).isEqualTo(record.id());
		assertThat(repository.findById(record.id())).hasValue(updated.get());
	}

	@Test
	void markReadFalseReturnsUpdatedRecord() {
		InboundNotification record = notification("svc-a", "TYPE_A", true);
		repository.save(record);

		Optional<InboundNotification> updated = repository.markRead(record.id(), false);

		assertThat(updated).isPresent();
		assertThat(updated.get().read()).isFalse();
	}

	@Test
	void markReadReturnsEmptyForUnknownId() {
		assertThat(repository.markRead(UUID.randomUUID(), true)).isEmpty();
	}

	@Test
	void deleteByIdRemovesExistingRecord() {
		InboundNotification record = notification("svc-a", "TYPE_A", false);
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
		repository.save(notification("svc-a", "TYPE_A", false));
		List<InboundNotification> result = repository.findAll(null, null, 10);

		assertThat(result).isUnmodifiable();
	}

	@Test
	void saveEvictsOldestEntryWhenStoreIsFull() {
		repository = new InMemoryInboundNotificationRepository(3);
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		InboundNotification oldest = notification("old-svc", "TYPE_A", false, base);
		InboundNotification middle = notification("mid-svc", "TYPE_A", false, base.plusSeconds(60));
		InboundNotification newest = notification("new-svc", "TYPE_A", false, base.plusSeconds(120));
		repository.save(oldest);
		repository.save(middle);
		repository.save(newest);

		// Store lleno: una entrada nueva evicta la más antigua
		InboundNotification extra = notification("extra-svc", "TYPE_A", false, base.plusSeconds(180));
		repository.save(extra);

		assertThat(repository.findById(oldest.id())).isEmpty();
		assertThat(repository.findById(middle.id())).isPresent();
		assertThat(repository.findById(newest.id())).isPresent();
		assertThat(repository.findById(extra.id())).isPresent();
		assertThat(repository.findAll(null, null, 10)).hasSize(3);
	}

}
