package com.videoteca.notifications.domain.repository;

import com.videoteca.notifications.domain.NotificationStatus;
import com.videoteca.notifications.domain.OutboundNotification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de notificaciones salientes.
 *
 * <p>La implementación actual ({@code InMemoryOutboundNotificationRepository})
 * persiste en memoria: los datos se pierden al reiniciar la aplicación.</p>
 */
public interface OutboundNotificationRepository {

	/**
	 * Almacena o actualiza una notificación por su id.
	 *
	 * @return el registro guardado
	 */
	OutboundNotification save(OutboundNotification notification);

	Optional<OutboundNotification> findById(UUID id);

	/**
	 * Lista notificaciones, de más reciente a más antigua.
	 *
	 * @param status filtro por estado, o {@code null} para todos
	 * @param limit  máximo de elementos a devolver
	 */
	List<OutboundNotification> findAll(NotificationStatus status, int limit);

	/**
	 * Elimina una notificación por id.
	 *
	 * @return {@code true} si se eliminó, {@code false} si no existía
	 */
	boolean deleteById(UUID id);

}
