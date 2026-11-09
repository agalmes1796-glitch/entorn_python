package com.videoteca.notifications.service.impl;

import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;
import com.videoteca.notifications.exception.NotificationNotFoundException;
import com.videoteca.notifications.model.Notification;
import com.videoteca.notifications.repository.NotificationRepository;
import com.videoteca.notifications.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementació del servei de notificacions sobre el repositori JPA.
 *
 * <p>Tot el mètode s'executa dins d'una transacció; les lectures podrien
 * ser transaccions de només lectura.</p>
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
    public NotificationResponse getById(UUID id) {
        return NotificationResponse.from(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(Boolean read) {
        List<Notification> notifications = (read == null)
                ? repository.findAll()
                : repository.findByRead(read);
        return notifications.stream().map(NotificationResponse::from).toList();
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
