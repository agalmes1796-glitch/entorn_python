package com.videoteca.movies.service.impl;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.entity.Movie;
import com.videoteca.movies.exception.ResourceNotFoundException;
import com.videoteca.movies.mapper.MovieMapper;
import com.videoteca.movies.repository.MovieRepository;
import com.videoteca.movies.service.MovieService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del CRUD de películas sobre {@link MovieRepository}.
 */
@Service
public class MovieServiceImpl implements MovieService {

    /** Nombre del recurso para los mensajes de error 404. */
    private static final String RESOURCE = "Pelicula";

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    public MovieServiceImpl(MovieRepository movieRepository, MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> findAll() {
        return movieRepository.findAll().stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse findById(Long id) {
        return movieRepository.findById(id)
                .map(movieMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    @Override
    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie saved = movieRepository.save(movieMapper.toEntity(request));
        return movieMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        movieMapper.updateEntity(movie, request);
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Un solo round-trip de carga: findById y, si existe, delete(entity)
        // (el entity queda gestionado por la persistencia, así Spring Data JPA
        // emite un único DELETE sin recargar la entidad, a diferencia de
        // deleteById, que haría un SELECT adicional).
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        movieRepository.delete(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> findByTituloContainingIgnoreCase(String titulo) {
        return movieRepository.findByTituloContainingIgnoreCase(titulo).stream()
                .map(movieMapper::toResponse)
                .toList();
    }
}
