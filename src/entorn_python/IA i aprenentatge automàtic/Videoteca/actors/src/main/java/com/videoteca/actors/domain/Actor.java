package com.videoteca.actors.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Actor del catàleg de la videoteca.
 *
 * <p>Entitat persistida a la taula {@code actor} de la base de dades MariaDB
 * (BD {@code videoteca}). {@code nombre} i {@code apellidos} són obligatoris;
 * la resta de camps són opcionals. La validació de les peticions REST passa
 * pel DTO {@code ActorRequest} (que retorna un 400 si no és vàlid), i les
 * anotacions d'aquesta entitat actuen de xarxa de seguretat en persistir.</p>
 */
@Entity
@Table(name = "actor")
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String apellidos;

    /** Data de naixement (opcional); se serialitza com a ISO-8601 (yyyy-MM-dd). */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Size(max = 100)
    @Column(length = 100)
    private String nacionalidad;

    /** URL de la foto de l'actor (opcional). */
    @Size(max = 2048)
    @Column(length = 2048)
    private String foto;

    /** Biografia (opcional); columna TEXT per a textos llargs. */
    @Size(max = 65535)
    @Column(columnDefinition = "TEXT")
    private String biografia;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }
}
