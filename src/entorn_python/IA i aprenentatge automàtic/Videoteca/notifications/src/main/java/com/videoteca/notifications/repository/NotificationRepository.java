package com.videoteca.notifications.repository;

import com.videoteca.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositori JPA de notificacions.
 *
 * <p>Hereta les operacions bàsiques de {@link JpaRepository} i afegeix la
 * consulta derivada per filtrar per l'estat de lectura.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Retorna les notificacions segons l'estat de lectura:
     * {@code read=true} (llegides) o {@code read=false} (no llegides).
     */
    List<Notification> findByRead(boolean read);
}
