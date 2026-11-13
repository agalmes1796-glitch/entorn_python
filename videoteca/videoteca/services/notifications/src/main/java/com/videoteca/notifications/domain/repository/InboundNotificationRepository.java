package com.videoteca.notifications.domain.repository;

import com.videoteca.notifications.domain.InboundNotification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de notificaciones entrantes.
 *
 * <p>La implementación actual ({@code InMemoryInboundNotificationRepository})
 * persiste en memoria: los datos se pierden al reiniciar la aplicación.</p>
 */
public interface InboundNotificationRepository {

	/**
	 * Almacena o actualiza una notificación por su id.
	 *
	 * @return el registro guardado
	 */
	InboundNotification save(InboundNotification notification);

	Optional<InboundNotification> findById(UUID id);

	/**
	 * Lista notificaciones, de más reciente a más antigua.
	 *
	 * @param read  filtro por estado de lectura, o {@code null} para todos
	 * @param type  filtro por tipo, o {@code null} para todos
	 * @param limit máximo de elementos a devolver
	 */
	List<InboundNotification> findAll(Boolean read, String type, int limit);

	/**
	 * Elimina una notificación por id.
	 *
	 * @return {@code true} si se eliminó, {@code false} si no existía
	 */
	boolean deleteById(UUID id);

	/**
	 * Marca una notificación como leída o no leída.
	 *
	 * @return el registro actualizado, o vacío si no existe
	 */
	Optional<InboundNotification> markRead(UUID id, boolean read);

}
