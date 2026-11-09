package com.videoteca.movies.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.entity.Movie;
import com.videoteca.movies.exception.ResourceNotFoundException;
import com.videoteca.movies.mapper.MovieMapper;
import com.videoteca.movies.repository.MovieRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias del {@link MovieServiceImpl} con Mockito,
 * sin base de datos ni contexto de Spring.
 *
 * <p>El {@link MovieRepository} se simula (es el único colaborador con
 * comportamiento externo); el {@link MovieMapper} es un convertidor puro sin
 * dependencias, así que se usa el <b>real</b> para cubrir también la
 * transformación entidad↔DTO.
 */
@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    private final MovieMapper movieMapper = new MovieMapper();

    private MovieServiceImpl movieService;

    private MovieRequest request;

    @BeforeEach
    void setUp() {
        movieService = new MovieServiceImpl(movieRepository, movieMapper);

        request = new MovieRequest();
        request.setTitulo("Interestelar");
        request.setSinopsis("Un viaje más allá del sistema solar.");
        request.setAnioEstreno(2014);
        request.setDuracionMinutos(148);
        request.setGeneroId(1L);
        request.setDirectorId(null);
    }

    @Test
    void create_debeMapearYPersistirYDevolverLaRespuesta() {
        // Simula lo que haría la BD: devolver la entidad persistida.
        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse result = movieService.create(request);

        // El id lo asigna la BD (IDENTITY), no el servicio: aquí es null.
        assertThat(result.id()).isNull();
        assertThat(result.titulo()).isEqualTo("Interestelar");
        assertThat(result.sinopsis()).isEqualTo("Un viaje más allá del sistema solar.");
        assertThat(result.anioEstreno()).isEqualTo(2014);
        assertThat(result.duracionMinutos()).isEqualTo(148);
        assertThat(result.generoId()).isEqualTo(1L);
        assertThat(result.directorId()).isNull();
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void findById_debeDevolverLaRespuestaMapeadaSiExiste() {
        Movie movie = new Movie("Interestelar", "Sinopsis", 2014, 148, 1L, null);
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));

        MovieResponse result = movieService.findById(42L);

        assertThat(result).isEqualTo(new MovieResponse(null, "Interestelar", "Sinopsis", 2014, 148, 1L, null));
    }

    @Test
    void findById_debeLanzarNotFoundSiNoExiste() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_debeSustituirLosCamposDeLaEntidadExistente() {
        Movie existing = new Movie("Título viejo", "Sinopsis vieja", 2010, 100, 2L, null);
        when(movieRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenReturn(existing);

        MovieResponse result = movieService.update(42L, request);

        // La entidad real ha sido mutada por el mapper.
        assertThat(existing.getTitulo()).isEqualTo("Interestelar");
        assertThat(existing.getSinopsis()).isEqualTo("Un viaje más allá del sistema solar.");
        assertThat(existing.getAnioEstreno()).isEqualTo(2014);
        assertThat(existing.getDuracionMinutos()).isEqualTo(148);
        assertThat(existing.getGeneroId()).isEqualTo(1L);
        assertThat(existing.getDirectorId()).isNull();

        assertThat(result.titulo()).isEqualTo("Interestelar");
        assertThat(result.generoId()).isEqualTo(1L);
        verify(movieRepository).save(existing);
    }

    @Test
    void update_debeLanzarNotFoundSiNoExiste() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void delete_debeEliminarSiExiste() {
        Movie movie = new Movie("Interestelar", "Sinopsis", 2014, 148, 1L, null);
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));

        movieService.delete(42L);

        // Un solo round-trip de carga: findById + delete(entity) gestionado
        // (sin el existsById previo ni el SELECT extra de deleteById).
        verify(movieRepository).delete(movie);
        verify(movieRepository, never()).deleteById(any());
    }

    @Test
    void delete_debeLanzarNotFoundSiNoExiste() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(movieRepository, never()).delete(any(Movie.class));
        verify(movieRepository, never()).deleteById(any());
    }

    @Test
    void findAll_debeMapearTodasLasEntidades() {
        Movie movie1 = new Movie("Interstellar", null, 2014, 148, 1L, null);
        Movie movie2 = new Movie("Inception", null, 2010, 148, 1L, null);
        when(movieRepository.findAll()).thenReturn(List.of(movie1, movie2));

        List<MovieResponse> result = movieService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).titulo()).isEqualTo("Interstellar");
        assertThat(result.get(0).anioEstreno()).isEqualTo(2014);
        assertThat(result.get(1).titulo()).isEqualTo("Inception");
        assertThat(result.get(1).anioEstreno()).isEqualTo(2010);
    }

    @Test
    void search_debeMapearResultadosDeLaBúsqueda() {
        Movie movie = new Movie("Interestelar", null, 2014, 148, 1L, null);
        when(movieRepository.findByTituloContainingIgnoreCase("inter")).thenReturn(List.of(movie));

        List<MovieResponse> result = movieService.findByTituloContainingIgnoreCase("inter");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).titulo()).isEqualTo("Interestelar");
        assertThat(result.get(0).generoId()).isEqualTo(1L);
    }
}
