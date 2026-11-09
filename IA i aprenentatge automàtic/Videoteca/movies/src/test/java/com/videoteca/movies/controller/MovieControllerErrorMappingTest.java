package com.videoteca.movies.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoteca.movies.entity.Movie;
import com.videoteca.movies.exception.GlobalExceptionHandler;
import com.videoteca.movies.mapper.MovieMapper;
import com.videoteca.movies.repository.MovieRepository;
import com.videoteca.movies.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba del mapeo de {@link DataIntegrityViolationException} → 409 vía
 * MockMvc.
 *
 * <p>En H2 (tests) no se generan constraints: las entidades no declaran
 * relaciones JPA hacia {@code genero}/{@code director}, así que la FK de la
 * BD real no se replica. Para fijar el contrato 409 se usa un MockMvc en modo
 * {@code standaloneSetup} con un {@link MovieRepository} simulado (Mockito,
 * sin contexto de Spring) cuyo {@code save} lanza la excepción, y el
 * {@link GlobalExceptionHandler} real como controlador advice.
 *
 * <p><b>Contrato de JSON:</b> al ser un setup standalone (sin contexto de
 * Spring, y por tanto sin el autoconfigure de Jackson de Boot) se registra
 * explícitamente el {@link JacksonJsonHttpMessageConverter} de Spring 7 con un
 * {@link JsonMapper} en el que se fija
 * {@link PropertyNamingStrategies#SNAKE_CASE}, replicando así la naming
 * strategy global de producción
 * ({@code spring.jackson.property-naming-strategy: SNAKE_CASE} en
 * {@code application.yml}). El cuerpo de petición y la respuesta de error van
 * en snake_case como en el runtime: {@code field_errors} (componente del
 * record {@code ApiError}) con claves de mapa en camelCase.
 *
 * <p>En la BD real (MariaDB) esta excepción ocurre, p. ej., al crear una
 * película con un {@code generoId} o {@code directorId} inexistente.
 */
class MovieControllerErrorMappingTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MovieRepository repository = Mockito.mock(MovieRepository.class);
        when(repository.save(any(Movie.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails"));

        MovieController controller =
                new MovieController(new MovieServiceImpl(repository, new MovieMapper()));

        // Contracto con el runtime: Jackson 3 (tools.jackson) con SNAKE_CASE
        // global, igual que la propiedad de application.yml.
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder()
                        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .build());

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jsonConverter)
                .build();
    }

    @Test
    void post_conViolacionDeIntegridad_debeDevolver409ConMensajeEstable() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Genero inexistente",
                                  "anio_estreno": 2020,
                                  "duracion_minutos": 120,
                                  "genero_id": 9999,
                                  "director_id": null
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Violación de una restricción de integridad (generoId/directorId inexistente u otra constraint). Revisa los datos."))
                // field_errors existe y está vacío (nunca null)
                .andExpect(jsonPath("$.field_errors").isEmpty());
    }
}
