package com.videoteca.actors.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Entrada de la filmografía de un actor (película, año y papel interpretado).
 *
 * <p>{@code @Embeddable}: se persiste dentro de la tabla {@code actor_filmography}
 * generada por el {@code @ElementCollection} de {@link Actor#filmography},
 * sin identidad propia.
 */
@Embeddable
public class FilmographyEntry {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "film_year")
    private Integer year;

    @Column(name = "role", length = 120)
    private String role;

    protected FilmographyEntry() {
        // Requerido por JPA
    }

    public FilmographyEntry(String title, Integer year, String role) {
        this.title = title;
        this.year = year;
        this.role = role;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
