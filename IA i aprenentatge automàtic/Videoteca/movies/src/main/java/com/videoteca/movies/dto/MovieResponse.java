package com.videoteca.movies.dto;

/**
 * Representación de salida de una película ({@code 200 OK} / {@code 201 Created}).
 *
 * <p>Se usa un {@link record} inmutable: la respuesta nunca debe mutarse y Jackson
 * lo serializa directamente con sus componentes.
 *
 * <p><b>Formato del JSON en el wire: snake_case.</b> Por la naming strategy
 * global de Jackson ({@code spring.jackson.property-naming-strategy:
 * SNAKE_CASE} en {@code application.yml}, contrato con el microservicio
 * frontend), el JSON sale con las claves {@code anio_estreno},
 * {@code duracion_minutos}, {@code genero_id} y {@code director_id} (los
 * nombres de columna de la tabla {@code pelicula}). Los nombres de los
 * componentes Java se mantienen en camelCase (solo cambia el alias en el
 * JSON).
 */
public record MovieResponse(
        Long id,
        String titulo,
        String sinopsis,
        Integer anioEstreno,
        Integer duracionMinutos,
        Long generoId,
        Long directorId) {
}
