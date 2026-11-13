package com.videoteca.notifications.service.impl;

import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;
import com.videoteca.notifications.exception.NotificationNotFoundException;
import com.videoteca.notifications.model.Notification;
import com.videoteca.notifications.repository.NotificationRepository;
import com.videoteca.notifications.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementació del servei de notificacions sobre el repositori JPA.
 *
 * <p>Tot el mètode s'executa dins d'una transacció: les lectures
 * ({@link #getById}, {@link #getAll}) com a transacció de només lectura
 * ({@code @Transactional(readOnly = true)}), i les escriptures ({@code create},
 * {@code update}, {@code delete}, {@code markAsRead}) com a transacció normal.</p>
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationResponse create(NotificationRequest request) {
        Notification notification =
                new Notification(request.title(), request.message(), request.type());
        return NotificationResponse.from(repository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID id) {
        return NotificationResponse.from(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(Boolean read, Pageable pageable) {
        Page<Notification> notifications = (read == null)
                ? repository.findAll(pageable)
                : repository.findByRead(read, pageable);
        return notifications.map(NotificationResponse::from);
    }

    @Override
    public NotificationResponse update(UUID id, NotificationRequest request) {
        Notification notification = findByIdOrThrow(id);
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setType(request.type());
        return NotificationResponse.from(repository.save(notification));
    }

    @Override
    public void delete(UUID id) {
        repository.delete(findByIdOrThrow(id));
    }

    @Override
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = findByIdOrThrow(id);
        notification.setRead(true);
        return NotificationResponse.from(repository.save(notification));
    }

    /** Carrega l'entitat per l'id o llança {@link NotificationNotFoundException}. */
    private Notification findByIdOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
