package com.videoteca.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Entitat JPA que representa una notificació persistida a la taula
 * {@code notifications} de la base de dades {@code videoteca}.
 *
 * <p>La columna {@code read} és una paraula clau reservada en MariaDB;
 * Hibernate la delimita automàticament en el SQL generat per al dialecte.</p>
 */
@Entity
@Table(name = "notifications")
public class Notification {

    /** Id únic generat pel proveïdor persistència (estratègia UUID de Jakarta Persistence 3.2+). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type = NotificationType.INFO;

    /** Estat de lectura: {@code false} per defecte (notificació nova). */
    @Column(nullable = false)
    private boolean read = false;

    /** Instant de creació (estableix {@code @PrePersist}). */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Instant de darrera modificació (estableix {@code @PrePersist}/{@code @PreUpdate}). */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Constructor sense arguments, exigida per l'especificació JPA. */
    protected Notification() {
    }

    public Notification(String title, String message, NotificationType type) {
        this.title = title;
        this.message = message;
        this.type = (type == null) ? NotificationType.INFO : type;
    }

    /** Inicialitza els timestamps abans de la primera inserció. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /** Refresha el timestamp de modificació abans de cada actualització. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    /** Només s'usa en proves per simular l'id generat pel proveïdor persistència. */
    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
