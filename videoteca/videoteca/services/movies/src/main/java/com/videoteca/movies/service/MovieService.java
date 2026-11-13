package com.videoteca.movies.service;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.entity.Movie;
import com.videoteca.movies.exception.MovieNoEncontradaException;
import com.videoteca.movies.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio de películas.
 *
 * <p>Capa intermedia entre el {@code controller} (solo HTTP) y el
 * {@code repository} (persistencia).
 */
@Service
public class MovieService {

    private final MovieRepository repository;

    public MovieService(MovieRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista todas las películas, o filtra por {@code search} si se indica.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> findAll(String search) {
        if (search == null || search.isBlank()) {
            return repository.findAll().stream()
                    .map(MovieResponse::fromEntity)
                    .toList();
        }
        return repository.searchByTitle(buildSearchPattern(search)).stream()
                .map(MovieResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse findById(Long id) {
        return repository.findById(id)
                .map(MovieResponse::fromEntity)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie(
                request.title(),
                request.director(),
                request.releaseYear(),
                request.genre(),
                request.description(),
                request.rating());
        return MovieResponse.fromEntity(repository.save(movie));
    }

    /**
     * Sustitución completa de una película existente.
     */
    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = repository.findById(id).orElseThrow(() -> notFound(id));
        movie.setTitle(request.title());
        movie.setDirector(request.director());
        movie.setReleaseYear(request.releaseYear());
        movie.setGenre(request.genre());
        movie.setDescription(request.description());
        movie.setRating(request.rating());
        // Se fija updatedAt explícitamente para que la respuesta 200 refleje la
        // modificación real, eliminando la dependencia del flush de @PreUpdate
        // (que se ejecuta al commit, ya tras la serialización de la respuesta).
        movie.setUpdatedAt(LocalDateTime.now());
        // Se persiste por dirty-checking al finalizar la transacción.
        return MovieResponse.fromEntity(movie);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    /**
     * Construye un patrón LIKE seguro: escapando los comodines
     * {@code %}, {@code _} y el carácter de escape {@code \} del input del
     * usuario, y forzando case-insensitivity con {@code lower()}.
     */
    private String buildSearchPattern(String search) {
        String escaped = search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .toLowerCase(Locale.ROOT);
        return "%" + escaped + "%";
    }

    private MovieNoEncontradaException notFound(Long id) {
        return new MovieNoEncontradaException("Pelicula con id %d no encontrada".formatted(id));
    }
}
