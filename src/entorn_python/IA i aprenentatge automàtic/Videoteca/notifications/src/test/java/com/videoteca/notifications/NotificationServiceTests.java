package com.videoteca.notifications;

import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;
import com.videoteca.notifications.exception.NotificationNotFoundException;
import com.videoteca.notifications.model.Notification;
import com.videoteca.notifications.model.NotificationType;
import com.videoteca.notifications.repository.NotificationRepository;
import com.videoteca.notifications.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves d'unitat del {@link NotificationServiceImpl} amb el repositori simulat.
 *
 * <p>Cobreix les operacions CRUD, la marca de lectura i els casos
 * {@link NotificationNotFoundException} quan l'id no existeix.</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    private static final UUID ID = UUID.fromString("aaaaaa11-2222-3333-4444-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    private static final NotificationRequest REQUEST =
            new NotificationRequest("Títol", "Missatge", NotificationType.ERROR);

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationServiceImpl service;

    /** Entitat amb id i timestamps, com si l'hagués persistit JPA. */
    private static Notification persistedEntity(String title, String message,
                                               NotificationType type, boolean read) {
        Notification notification = new Notification(title, message, type);
        notification.setId(ID);
        notification.setRead(read);
        notification.setCreatedAt(NOW);
        notification.setUpdatedAt(NOW);
        return notification;
    }

    // ---------- create ----------

    @Test
    void createPersistsANewNotification() {
        // L'answer simula el proveïdor persistència: id assignat i timestamps
        // inicialitzats (el que faria @PrePersist dins d'una transacció real).
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification persisted = invocation.getArgument(0);
            persisted.setId(ID);
            persisted.setCreatedAt(NOW);
            persisted.setUpdatedAt(NOW);
            return persisted;
        });

        NotificationResponse response = service.create(REQUEST);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("Títol");
        assertThat(response.message()).isEqualTo("Missatge");
        assertThat(response.type()).isEqualTo(NotificationType.ERROR);
        assertThat(response.read()).isFalse();
        assertThat(response.createdAt()).isEqualTo(NOW);
        verify(repository).save(any(Notification.class));
    }

    @Test
    void createDefaultsTheTypeToInfo() {
        NotificationRequest withoutType = new NotificationRequest("Títol", "Missatge", null);
        Notification saved = persistedEntity("Títol", "Missatge", NotificationType.INFO, false);
        when(repository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse response = service.create(withoutType);

        assertThat(response.type()).isEqualTo(NotificationType.INFO);
    }

    // ---------- getById ----------

    @Test
    void getByIdReturnsTheNotification() {
        when(repository.findById(ID)).thenReturn(Optional.of(persistedEntity("T", "M", NotificationType.INFO, false)));

        NotificationResponse response = service.getById(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("T");
        verify(repository).findById(ID);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(ID))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }

    // ---------- getAll ----------

    @Test
    void getAllWithoutFilterReturnsAllNotifications() {
        when(repository.findAll()).thenReturn(
                List.of(persistedEntity("T1", "M1", NotificationType.INFO, false),
                        persistedEntity("T2", "M2", NotificationType.SUCCESS, true)));

        List<NotificationResponse> all = service.getAll(null);

        assertThat(all).hasSize(2);
        assertThat(all).extracting(NotificationResponse::title).containsExactly("T1", "T2");
        verify(repository).findAll();
        verify(repository, never()).findByRead(anyBoolean());
    }

    @Test
    void getAllWithReadFilterQueriesByRead() {
        when(repository.findByRead(false)).thenReturn(List.of(persistedEntity("T", "M", NotificationType.INFO, false)));

        List<NotificationResponse> unread = service.getAll(false);

        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).read()).isFalse();
        verify(repository).findByRead(false);
        verify(repository, never()).findAll();
    }

    // ---------- update ----------

    @Test
    void updateReplacesTheNotificationFields() {
        Notification existing = persistedEntity("Títol antic", "Missatge antic", NotificationType.INFO, true);
        when(repository.findById(ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.update(ID, REQUEST);

        assertThat(response.title()).isEqualTo("Títol");
        assertThat(response.message()).isEqualTo("Missatge");
        assertThat(response.type()).isEqualTo(NotificationType.ERROR);
        // L'estat de lectura no l'altera una actualització.
        assertThat(response.read()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ID, REQUEST))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesTheExistingNotification() {
        Notification existing = persistedEntity("T", "M", NotificationType.INFO, false);
        when(repository.findById(ID)).thenReturn(Optional.of(existing));

        service.delete(ID);

        verify(repository).delete(existing);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(repository, never()).delete(any(Notification.class));
    }

    // ---------- markAsRead ----------

    @Test
    void markAsReadSetsTheFlagAndPersists() {
        Notification existing = persistedEntity("T", "M", NotificationType.INFO, false);
        when(repository.findById(ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.markAsRead(ID);

        assertThat(response.read()).isTrue();
        assertThat(existing.isRead()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void markAsReadThrowsWhenNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(ID))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
