package com.videoteca.notifications.domain.repository.inmemory;

import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;
import com.videoteca.notifications.domain.repository.OutboundNotificationRepository;
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
 * Implementación EN MEMORIA de {@link OutboundNotificationRepository}
 * basada en {@link ConcurrentHashMap}.
 *
 * <p><b>Los datos se pierden al reiniciar la aplicación.</b> El crecimiento
 * está acotado: al llegar al tope {@code notifications.store.max-size},
 * cada {@code save} de una entrada nueva evicta la más antigua
 * (por {@code createdAt}) y lo loguea.</p>
 */
@Repository
public class InMemoryOutboundNotificationRepository implements OutboundNotificationRepository {

	private static final Logger log = LoggerFactory.getLogger(InMemoryOutboundNotificationRepository.class);

	private final Map<UUID, OutboundNotification> store = new ConcurrentHashMap<>();

	private final int maxSize;

	public InMemoryOutboundNotificationRepository(@Value("${notifications.store.max-size:10000}") int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public OutboundNotification save(OutboundNotification notification) {
		evictOldestIfNeeded(notification.id());
		store.put(notification.id(), notification);
		return notification;
	}

	@Override
	public Optional<OutboundNotification> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<OutboundNotification> findAll(NotificationStatus status, int limit) {
		return store.values().stream()
			.filter(notification -> status == null || notification.status() == status)
			.sorted(Comparator.comparing(OutboundNotification::createdAt).reversed())
			.limit(limit)
			.toList();
	}

	@Override
	public boolean deleteById(UUID id) {
		return store.remove(id) != null;
	}

	/**
	 * Si el store está lleno y la entrada a guardar es nueva, evicta la más
	 * antigua (por {@code createdAt}). Las actualizaciones (mismo id, p. ej.
	 * un resend) no evictan nada.
	 */
	private void evictOldestIfNeeded(UUID newId) {
		if (store.containsKey(newId) || store.size() < maxSize) {
			return;
		}
		store.entrySet().stream()
			.min(Comparator.comparing(entry -> entry.getValue().createdAt()))
			.ifPresent(entry -> {
				store.remove(entry.getKey());
				log.warn("Store de notificaciones salientes lleno (max-size={}): se evicta la más antigua id={}",
						maxSize, entry.getKey());
			});
	}

}
