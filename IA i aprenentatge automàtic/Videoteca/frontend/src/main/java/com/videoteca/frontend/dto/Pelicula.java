package com.videoteca.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pel·lícula tal i com la veuen els clients del microservici.
 *
 * <p><b>Suposició sobre la forma del JSON:</b> el servei "movies" (i per tant el
 * gateway, que el redirigeix) retorna els camps amb els mateixos noms que les
 * columnes de la taula {@code pelicula}, és a dir, snake_case
 * ({@code anio_estreno}, {@code duracion_minutos}, {@code genero_id},
 * {@code director_id}). Si en el futur el servei tornés camelCase, caldria
 * ajustar els {@link JsonProperty} corresponents.</p>
 */
public record Pelicula(
        @JsonProperty("id") long id,
        @JsonProperty("titulo") String titulo,
        @JsonProperty("sinopsis") String sinopsis,
        @JsonProperty("anio_estreno") int anioEstreno,
        @JsonProperty("duracion_minutos") int duracionMinutos,
        @JsonProperty("genero_id") long generoId,
        @JsonProperty("director_id") long directorId
) {
}
