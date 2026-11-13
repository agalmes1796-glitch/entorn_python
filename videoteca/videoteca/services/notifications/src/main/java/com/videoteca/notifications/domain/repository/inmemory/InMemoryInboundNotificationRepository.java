package com.videoteca.notifications.domain.repository.inmemory;

import com.videoteca.notifications.domain.InboundNotification;
import com.videoteca.notifications.domain.repository.InboundNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación EN MEMORIA de {@link InboundNotificationRepository}
 * basada en {@link ConcurrentHashMap}.
 *
 * <p><b>Los datos se pierden al reiniciar la aplicación.</b> El crecimiento
 * está acotado: al llegar al tope {@code notifications.store.max-size},
 * cada {@code save} de una entrada nueva evicta la más antigua
 * (por {@code receivedAt}) y lo loguea.</p>
 */
@Repository
public class InMemoryInboundNotificationRepository implements InboundNotificationRepository {

	private static final Logger log = LoggerFactory.getLogger(InMemoryInboundNotificationRepository.class);

	private final Map<UUID, InboundNotification> store = new ConcurrentHashMap<>();

	private final int maxSize;

	public InMemoryInboundNotificationRepository(@Value("${notifications.store.max-size:10000}") int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public InboundNotification save(InboundNotification notification) {
		evictOldestIfNeeded(notification.id());
		store.put(notification.id(), notification);
		return notification;
	}

	@Override
	public Optional<InboundNotification> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<InboundNotification> findAll(Boolean read, String type, int limit) {
		return store.values().stream()
			.filter(notification -> read == null || notification.read() == read)
			.filter(notification -> type == null || notification.type().equals(type))
			.sorted(Comparator.comparing(InboundNotification::receivedAt).reversed())
			.limit(limit)
			.toList();
	}

	@Override
	public boolean deleteById(UUID id) {
		return store.remove(id) != null;
	}

	/**
	 * Marca como leída/no leída de forma ATÓMICA sobre el map
	 * ({@code compute}): sin lecturas/escrituras no sincronizadas por fuera.
	 */
	@Override
	public Optional<InboundNotification> markRead(UUID id, boolean read) {
		return Optional.ofNullable(store.compute(id, (key, current) -> current == null ? null : current.withRead(read)));
	}

	/**
	 * Si el store está lleno y la entrada a guardar es nueva, evicta la más
	 * antigua (por {@code receivedAt}).
	 */
	private void evictOldestIfNeeded(UUID newId) {
		if (store.containsKey(newId) || store.size() < maxSize) {
			return;
		}
		store.entrySet().stream()
			.min(Comparator.comparing(entry -> entry.getValue().receivedAt()))
			.ifPresent(entry -> {
				store.remove(entry.getKey());
				log.warn("Store de notificaciones entrantes lleno (max-size={}): se evicta la más antigua id={}",
						maxSize, entry.getKey());
			});
	}

}
