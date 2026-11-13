package com.videoteca.actors.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad de persistencia para la tabla {@code actors}.
 *
 * <p>La filmografía del actor se modela como {@code @ElementCollection} de
 * {@link FilmographyEntry}, de modo que este microservicio cubre de forma
 * autocontenida la información de películas asociada al actor, sin dependencias
 * de otros servicios.
 *
 * <p>Los campos {@code createdAt} y {@code updatedAt} se mantienen de forma
 * automática mediante los callbacks de ciclo de vida JPA
 * ({@code @PrePersist} / {@code @PreUpdate}).
 */
@Entity
@Table(name = "actors")
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place", length = 120)
    private String birthPlace;

    @Column(name = "biography", length = 1000)
    private String biography;

    // @BatchSize mitiga el N+1 de la colección EAGER: Hibernate carga la
    // filmografía de hasta 50 actores por consulta IN, en vez de una por actor.
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "actor_filmography",
            joinColumns = @JoinColumn(name = "actor_id"))
    private List<FilmographyEntry> filmography = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Actor() {
        // Requerido por JPA
    }

    public Actor(String name,
                 LocalDate birthDate,
                 String birthPlace,
                 String biography,
                 List<FilmographyEntry> filmography) {
        this.name = name;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.biography = biography;
        if (filmography != null) {
            this.filmography = new ArrayList<>(filmography);
        }
    }

    /**
     * Se fija la fecha de creación (y la de última actualización)
     * la primera vez que la entidad se persiste.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Actualiza la fecha de última modificación en cada actualización.
     */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    /** Permite fijar el id (util, p. ej., en tests). */
    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<FilmographyEntry> getFilmography() {
        return filmography;
    }

    public void setFilmography(List<FilmographyEntry> filmography) {
        this.filmography = filmography != null ? new ArrayList<>(filmography) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Permite fijar la fecha de última modificación (la asigna el servicio). */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
