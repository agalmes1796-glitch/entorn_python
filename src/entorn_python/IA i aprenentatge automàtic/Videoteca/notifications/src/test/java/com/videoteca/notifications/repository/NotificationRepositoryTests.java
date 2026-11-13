package com.videoteca.notifications.repository;

import com.videoteca.notifications.model.Notification;
import com.videoteca.notifications.model.NotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves d'integració del {@link NotificationRepository} contra H2 en memòria
 * (el {@code application.yml} de proves usa {@code MODE=MYSQL} per aproximar el
 * dialecte de MariaDB, i el client d'Eureka ja hi està desactivat): verifiquen
 * el contracte de persistència de l'entitat (generació de l'id i timestamps mitjançant
 * {@code @PrePersist}/{@code @PreUpdate}), el viatge de prova de la columna
 * reservada {@code read} i el llistat paginat.
 */
@DataJpaTest
class NotificationRepositoryTests {

    @Autowired
    private NotificationRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    /** save assigna l'id (UUID) i els timestamps mitjançant {@code @PrePersist}. */
    @Test
    void saveGeneratesTheIdAndFillsTheTimestamps() {
        Notification saved = repository.save(
                new Notification("Títol", "Missatge", NotificationType.INFO));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        // @PrePersist omple ambdós amb el mateix instant.
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    /** findByRead filtra correctament pel round-trip de la columna reservada read. */
    @Test
    void findByReadFiltersByTheReadFlag() {
        Notification unread = repository.save(
                new Notification("No llegida", "M1", NotificationType.INFO));
        Notification read = repository.save(
                new Notification("Llegida", "M2", NotificationType.SUCCESS));
        read.setRead(true);

        Page<Notification> unreadPage = repository.findByRead(false, Pageable.unpaged());
        Page<Notification> readPage = repository.findByRead(true, Pageable.unpaged());

        assertThat(unreadPage.getContent()).containsExactly(unread);
        assertThat(readPage.getContent()).containsExactly(read);
    }

    /**
     * Dirty checking: modificar una entitat gestionada (sense cridar save)
     * actualitza {@code updatedAt} via {@code @PreUpdate}.
     */
    @Test
    void updatingWithoutSaveRefreshesUpdatedAt() throws InterruptedException {
        Notification saved = repository.save(
                new Notification("Títol", "Missatge", NotificationType.INFO));
        Instant createdAt = saved.getCreatedAt();
        Instant updatedAtBefore = saved.getUpdatedAt();

        // Espera per garantir que el rellotge avanci més enllà de la seva resolució.
        Thread.sleep(50);
        saved.setTitle("Títol canviat");
        entityManager.flush(); // persisteix el canvi per dirty checking, sense save

        Notification reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getTitle()).isEqualTo("Títol canviat");
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
        // Gràcies a l'espera, l'updatedAt és estrictament posterior al createdAt.
        assertThat(reloaded.getUpdatedAt()).isAfter(createdAt);
    }

    /** findAll(Pageable) respecta la mida de la pàgina i l'ordre descendent per createdAt. */
    @Test
    void findAllRespectsSizeAndDescendingOrder() {
        Instant base = Instant.parse("2026-11-01T00:00:00Z");
        for (int i = 0; i < 8; i++) {
            // createdAt explícita i distinta: @PrePersist la respecta si ja és no-null.
            Notification notification = new Notification("T" + i, "M" + i, NotificationType.INFO);
            notification.setCreatedAt(base.plusSeconds(i));
            repository.save(notification);
        }

        Page<Notification> page = repository.findAll(
                PageRequest.of(0, 5, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(8);
        assertThat(page.getTotalPages()).isEqualTo(2);
        // Les més recents primer.
        assertThat(page.getContent())
                .extracting(Notification::getTitle)
                .containsExactly("T7", "T6", "T5", "T4", "T3");
    }
}
