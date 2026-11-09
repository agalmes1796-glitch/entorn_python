package com.videoteca.movies;

import static org.assertj.core.api.Assertions.assertThat;

import com.videoteca.movies.controller.MovieController;
import com.videoteca.movies.repository.MovieRepository;
import com.videoteca.movies.service.MovieService;
import com.videoteca.movies.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de carga de contexto completo.
 *
 * <p>Arranca el ApplicationContext con todas las dependencias cableadas
 * (controller → service → repository → JPA sobre H2 en memoria, ver
 * {@code src/test/resources/application.yml}). Si falta un bean, una
 * anotación incorrecta o un conflicto de inyección, el contexto no carga
 * y la prueba falla.
 */
@SpringBootTest
class MoviesApplicationTests {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieController movieController;

    @Test
    void contextLoads() {
        assertThat(movieService).isInstanceOf(MovieServiceImpl.class);
        assertThat(movieRepository).isNotNull();
        assertThat(movieController).isNotNull();
    }
}
