package com.videoteca.movies.service;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.entity.Movie;
import com.videoteca.movies.exception.MovieNoEncontradaException;
import com.videoteca.movies.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link MovieService} (Mockito, sin Spring ni base de datos).
 */
@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    private static final Long ID = 42L;

    @Mock
    private MovieRepository repository;

    @InjectMocks
    private MovieService movieService;

    /** Película existente en el "repositorio" (con id fijo para los tests). */
    private Movie movieExistente() {
        Movie movie = new Movie(
                "El Padrino",
                "Francis Ford Coppola",
                1972,
                "Drama",
                "Saga familiar de la mafia neoyorquina.",
                new BigDecimal("8.50"));
        movie.setId(ID);
        return movie;
    }

    private MovieRequest requestCompleto() {
        return new MovieRequest(
                "Cazafantasmas",
                "Ivan Reitman",
                1984,
                "Comedia",
                "Cazadores de fantasmas en Nueva York.",
                new BigDecimal("7.25"));
    }

    private MovieRequest requestParcialSinOpcionales() {
        return new MovieRequest("Duna", null, 1984, null, null, null);
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_debePersistirYDevolverLaPeliculaCreada() {
        when(repository.save(any(Movie.class)))
                .thenAnswer(invocation -> {
                    Movie movie = invocation.getArgument(0);
                    return movie; // el id lo asigna el generador real (IDENTITY); aquí devolvemos la misma entidad
                });

        MovieResponse response = movieService.create(requestCompleto());

        assertThat(response.title()).isEqualTo("Cazafantasmas");
        assertThat(response.director()).isEqualTo("Ivan Reitman");
        assertThat(response.releaseYear()).isEqualTo(1984);
        assertThat(response.genre()).isEqualTo("Comedia");
        assertThat(response.rating()).isEqualByComparingTo("7.25");
        verify(repository).save(any(Movie.class));
    }

    @Test
    void createConCamposOpcionalesNulos_debePersistirlosNulos() {
        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        when(repository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        movieService.create(requestParcialSinOpcionales());

        verify(repository).save(captor.capture());
        Movie persistida = captor.getValue();
        assertThat(persistida.getTitle()).isEqualTo("Duna");
        assertThat(persistida.getDirector()).isNull();
        assertThat(persistida.getGenre()).isNull();
        assertThat(persistida.getDescription()).isNull();
        assertThat(persistida.getRating()).isNull();
    }

    // ------------------------------------------------------------- findAll

    @Test
    void findAllSinBusqueda_debeDevolverTodasLasPelis() {
        when(repository.findAll()).thenReturn(List.of(movieExistente()));

        List<MovieResponse> response = movieService.findAll(null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("El Padrino");
        verify(repository, never()).searchByTitle(any());
    }

    @Test
    void findAllConBusqueda_debeDelegarALaBuscaPorTitulo() {
        when(repository.searchByTitle("%el padrino%")).thenReturn(List.of(movieExistente()));

        List<MovieResponse> response = movieService.findAll("El Padrino");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("El Padrino");
        verify(repository, never()).findAll();
    }

    @Test
    void findAllConBusqueda_debeEscaparLosComodinesLikeDelInput() {
        when(repository.searchByTitle(any())).thenReturn(List.of());

        movieService.findAll("%_\\");

        // El input debe llegar al query como patrón literal, no como comodines:
        // \ -> \\, % -> \%, _ -> \_ (y todo entre % ... %)
        verify(repository).searchByTitle("%\\%\\_\\\\%");
    }

    // ------------------------------------------------------------- findById

    @Test
    void findById_debeDevolverLaPeliculaSiExiste() {
        when(repository.findById(ID)).thenReturn(Optional.of(movieExistente()));

        MovieResponse response = movieService.findById(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("El Padrino");
        assertThat(response.director()).isEqualTo("Francis Ford Coppola");
        assertThat(response.releaseYear()).isEqualTo(1972);
    }

    @Test
    void findById_debeLanzarMovieNoEncontradaSiNoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.findById(ID))
                .isInstanceOf(MovieNoEncontradaException.class)
                .hasMessageContaining(String.valueOf(ID));
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_debeSustituirCompletamenteLosCampos() {
        when(repository.findById(ID)).thenReturn(Optional.of(movieExistente()));

        // PUT sin opcionales: reemplazo completo (los campos se vacían)
        MovieResponse response = movieService.update(ID, requestParcialSinOpcionales());

        assertThat(response.title()).isEqualTo("Duna");
        assertThat(response.director()).isNull();
        assertThat(response.releaseYear()).isEqualTo(1984);
        assertThat(response.genre()).isNull();
        assertThat(response.description()).isNull();
        assertThat(response.rating()).isNull();
    }

    @Test
    void update_debeLanzarMovieNoEncontradaSiNoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.update(ID, requestCompleto()))
                .isInstanceOf(MovieNoEncontradaException.class)
                .hasMessageContaining(String.valueOf(ID));
        verify(repository, never()).save(any(Movie.class));
    }

    // ---------------------------------------------------------------- delete

    @Test
    void delete_debeBorrarLaPeliculaSiExiste() {
        when(repository.existsById(ID)).thenReturn(true);

        movieService.delete(ID);

        verify(repository).deleteById(ID);
    }

    @Test
    void delete_debeLanzarMovieNoEncontradaSiNoExiste() {
        when(repository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> movieService.delete(ID))
                .isInstanceOf(MovieNoEncontradaException.class)
                .hasMessageContaining(String.valueOf(ID));
        verify(repository, never()).deleteById(any());
    }
}
