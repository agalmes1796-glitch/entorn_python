package com.videoteca.movies.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Petición de entrada para crear ({@code POST /api/movies} o
 * {@code POST /}) o actualizar ({@code PUT /api/movies/{id}} o
 * {@code PUT /{id}}) una película.
 *
 * <p><b>Formato del JSON en el wire: snake_case.</b> Por la naming strategy
 * global de Jackson ({@code spring.jackson.property-naming-strategy:
 * SNAKE_CASE} en {@code application.yml}, contrato con el microservicio
 * frontend), los cuerpos de petición aceptan las claves
 * {@code anio_estreno}, {@code duracion_minutos}, {@code genero_id} y
 * {@code director_id} (los nombres de columna de la tabla {@code pelicula}).
 * Los nombres de los campos Java se mantienen en camelCase (solo cambia el
 * alias en el JSON).
 *
 * <p>Validaciones (alineadas con el esquema real de la tabla {@code pelicula}):
 * <ul>
 *   <li>{@code titulo}: obligatorio y máx. 128 caracteres
 *       (columna {@code varchar(128)} — verificado en el esquema real; 255
 *       admitiría valores que la BD rechazaría con 409).</li>
 *   <li>{@code sinopsis}: opcional, máx. 65535 caracteres (columna {@code TEXT}).</li>
 *   <li>{@code anioEstreno}: opcional; 1000–9999 (columna {@code year(4)}).</li>
 *   <li>{@code duracionMinutos}: opcional; 0–65535 (columna {@code smallint unsigned}).</li>
 *   <li>{@code generoId}: obligatorio, porque la columna {@code genero_id} es
 *       {@code NOT NULL} en la tabla {@code pelicula}.</li>
 *   <li>{@code directorId}: opcional (columna {@code NULLABLE}).</li>
 * </ul>
 */
public class MovieRequest {

    @NotBlank(message = "El título no puede ser vacío ni nulo")
    @Size(max = 128, message = "El título no puede superar los 128 caracteres")
    private String titulo;

    @Size(max = 65535, message = "La sinopsis no puede superar los 65535 caracteres (columna TEXT)")
    private String sinopsis;

    @Min(value = 1000, message = "El año de estreno debe estar entre 1000 y 9999")
    @Max(value = 9999, message = "El año de estreno debe estar entre 1000 y 9999")
    private Integer anioEstreno;

    @Min(value = 0, message = "La duración no puede ser negativa")
    @Max(value = 65535, message = "La duración no puede superar los 65535 minutos (smallint unsigned)")
    private Integer duracionMinutos;

    @NotNull(message = "El género (generoId) es obligatorio")
    private Long generoId;

    private Long directorId;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public Integer getAnioEstreno() {
        return anioEstreno;
    }

    public void setAnioEstreno(Integer anioEstreno) {
        this.anioEstreno = anioEstreno;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Long getGeneroId() {
        return generoId;
    }

    public void setGeneroId(Long generoId) {
        this.generoId = generoId;
    }

    public Long getDirectorId() {
        return directorId;
    }

    public void setDirectorId(Long directorId) {
        this.directorId = directorId;
    }
}
