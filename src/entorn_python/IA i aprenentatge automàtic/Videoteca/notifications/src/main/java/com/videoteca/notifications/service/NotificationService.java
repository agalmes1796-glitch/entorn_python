package com.videoteca.notifications.service;

import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contracte del servei de notificacions.
 *
 * <p>L'implementació ({@code NotificationServiceImpl}) persisteix les
 * notificacions a MariaDB mitjançant el repositori JPA.</p>
 */
public interface NotificationService {

    /** Crea la notificació descrita a la petició i retorna la persistida (amb l'id generat). */
    NotificationResponse create(NotificationRequest request);

    /**
     * Retorna la notificació indicada per l'id.
     *
     * @throws com.videoteca.notifications.exception.NotificationNotFoundException si no existeix
     */
    NotificationResponse getById(UUID id);

    /**
     * Retorna totes les notificacions, o només les de l'estat de lectura
     * indicat quan {@code read} és no-null ({@code true}/{@code false}).
     */
    List<NotificationResponse> getAll(Boolean read);

    /**
     * Actualitza la notificació indicada per l'id amb els camps de la petició.
     *
     * @throws com.videoteca.notifications.exception.NotificationNotFoundException si no existeix
     */
    NotificationResponse update(UUID id, NotificationRequest request);

    /**
     * Elimina la notificació indicada per l'id.
     *
     * @throws com.videoteca.notifications.exception.NotificationNotFoundException si no existeix
     */
    void delete(UUID id);

    /**
     * Marca la notificació com a llegida ({@code read=true}).
     *
     * @throws com.videoteca.notifications.exception.NotificationNotFoundException si no existeix
     */
    NotificationResponse markAsRead(UUID id);
}
