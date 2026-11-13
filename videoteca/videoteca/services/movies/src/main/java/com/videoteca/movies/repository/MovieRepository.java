package com.videoteca.movies.repository;

import com.videoteca.movies.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio Spring Data JPA de la entidad {@link Movie}.
 */
public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * Búsqueda de títulos por subcadena, case-insensitive.
     *
     * <p>El patrón debe llegar ya escapado (caracteres comodín {@code %},
     * {@code _} y {@code \} delimitados) para que la búsqueda sea segura;
     * la cláusula {@code ESCAPE} lo garantiza.
     */
    @Query("select m from Movie m where lower(m.title) like :pattern escape '\\'")
    List<Movie> searchByTitle(@Param("pattern") String pattern);
}
