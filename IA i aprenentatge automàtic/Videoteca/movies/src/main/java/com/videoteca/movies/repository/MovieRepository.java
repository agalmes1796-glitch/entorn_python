package com.videoteca.movies.repository;

import com.videoteca.movies.entity.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA de {@link Movie}.
 *
 * <p>{@code findByTituloContainingIgnoreCase} genera un
 * {@code WHERE titulo LIKE '%?%' case-insensitive} (derivation por nombre de método).
 */
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByTituloContainingIgnoreCase(String titulo);
}
