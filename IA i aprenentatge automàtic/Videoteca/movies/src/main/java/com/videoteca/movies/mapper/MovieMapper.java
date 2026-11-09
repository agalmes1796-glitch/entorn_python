package com.videoteca.movies.mapper;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.entity.Movie;
import org.springframework.stereotype.Component;

/**
 * Conversión entre la entidad {@link Movie} y los DTOs de la API.
 *
 * <p>Clase de mapeo deliberada (sin librería externa): son 6 campos y no hay
 * justificación para introducir MapStruct u otra herramienta.
 */
@Component
public class MovieMapper {

    /** Construye una entidad nueva a partir de la petición (crear). */
    public Movie toEntity(MovieRequest request) {
        return new Movie(
                request.getTitulo(),
                request.getSinopsis(),
                request.getAnioEstreno(),
                request.getDuracionMinutos(),
                request.getGeneroId(),
                request.getDirectorId());
    }

    /** Copia los campos de la petición sobre una entidad existente (actualizar). */
    public void updateEntity(Movie existing, MovieRequest request) {
        existing.setTitulo(request.getTitulo());
        existing.setSinopsis(request.getSinopsis());
        existing.setAnioEstreno(request.getAnioEstreno());
        existing.setDuracionMinutos(request.getDuracionMinutos());
        existing.setGeneroId(request.getGeneroId());
        existing.setDirectorId(request.getDirectorId());
    }

    /** Convierte una entidad en la respuesta de la API. */
    public MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitulo(),
                movie.getSinopsis(),
                movie.getAnioEstreno(),
                movie.getDuracionMinutos(),
                movie.getGeneroId(),
                movie.getDirectorId());
    }
}
