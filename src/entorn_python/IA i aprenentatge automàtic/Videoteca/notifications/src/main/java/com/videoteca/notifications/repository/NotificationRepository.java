package com.videoteca.notifications.repository;

import com.videoteca.notifications.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositori JPA de notificacions.
 *
 * <p>Hereta les operacions bàsiques de {@link JpaRepository} (inclosa la
 * {@code findAll(Pageable)} paginada) i afegeix la consulta derivada per
 * filtrar per l'estat de lectura, també paginada.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Retorna les notificacions segons l'estat de lectura:
     * {@code read=true} (llegides) o {@code read=false} (no llegides),
     * aplicant la paginació de {@code pageable}.
     */
    Page<Notification> findByRead(boolean read, Pageable pageable);
}
