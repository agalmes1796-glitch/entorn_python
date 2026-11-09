package com.videoteca.movies.service;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import java.util.List;

/**
 * Contrato de negocio del CRUD de películas.
 *
 * <p>La capa de servicio lanza {@link com.videoteca.movies.exception.ResourceNotFoundException}
 * cuando la película pedida no existe; la traducción a HTTP 404 la hace el
 * {@code GlobalExceptionHandler}.
 */
public interface MovieService {

    /** Listado completo de películas. */
    List<MovieResponse> findAll();

    /** Busca una película por su id o lanza {@code ResourceNotFoundException}. */
    MovieResponse findById(Long id);

    /** Crea una película y devuelve la persistida (con id asignado). */
    MovieResponse create(MovieRequest request);

    /**
     * Actualiza una película existente (PUT semántica: todos los campos
     * de la petición sustituyen los actuales). Lanza 404 si no existe.
     */
    MovieResponse update(Long id, MovieRequest request);

    /** Elimina una película. Lanza 404 si no existe. */
    void delete(Long id);

    /** Búsqueda por título con LIKE (case-insensitive). */
    List<MovieResponse> findByTituloContainingIgnoreCase(String titulo);
}
