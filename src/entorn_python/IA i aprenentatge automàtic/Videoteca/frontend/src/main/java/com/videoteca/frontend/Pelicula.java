package com.videoteca.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de pel·lícula retornat per {@code GET /movies}.
 *
 * <p><b>Supòsit de mapeig:</b> els noms de les components coincideixen amb les
 * columnes de la taula {@code pelicula} ({@code id}, {@code titulo},
 * {@code sinopsis}, {@code anio_estreno}, {@code duracion_minutos},
 * {@code genero_id}, {@code director_id}) i es pressuposa que el servei
 * "movies" (i, per tant, el gateway que ho retransmèt) retorna el JSON amb
 * aquests mateixos noms en snake_case. Si el servei de movies canviés el format del
 * JSON (p. ex. camelCase), caldria afegir {@code @JsonProperty} a les
 * components.</p>
 *
 * <p>{@link JsonIgnoreProperties} ignora camps extra que el servei de movies
 * pugui afegir en el futur (p. ex. una imatge de portada).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pelicula(
        long id,
        String titulo,
        String sinopsis,
        int anio_estreno,
        int duracion_minutos,
        long genero_id,
        long director_id) {
}
