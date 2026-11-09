package com.videoteca.frontend;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueses del controlador {@code GET /movies} amb el client HTTP mockejat
 * (cap petició real al gateway).
 */
@WebMvcTest(MoviesController.class)
class MoviesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoviesClient moviesClient;

    @Test
    void retornaLesPeliculesQuanElGatewayResponCorrectament() throws Exception {
        given(moviesClient.fetchMovies()).willReturn(List.of(
                new Pelicula(1, "El viatger", "Una sinopsi qualsevol", 2020, 120, 7, 3)));

        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("El viatger"))
                .andExpect(jsonPath("$[0].sinopsis").value("Una sinopsi qualsevol"))
                .andExpect(jsonPath("$[0].anio_estreno").value(2020))
                .andExpect(jsonPath("$[0].duracion_minutos").value(120))
                .andExpect(jsonPath("$[0].genero_id").value(7))
                .andExpect(jsonPath("$[0].director_id").value(3));
    }

    @Test
    void retorna503AmbJsonDErrorQuanElGatewayNoEstDisponibile() throws Exception {
        String missatge = "No s'ha pogut contactar amb el gateway per consultar les pel·lícules: timeout";
        willThrow(new MoviesUnavailableException(missatge, null)).given(moviesClient).fetchMovies();

        String cos = mockMvc.perform(get("/movies"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(missatge))
                .andReturn().getResponse().getContentAsString();

        // La resposta no ha de fugar stack traces.
        assertThat(cos).doesNotContain("at ", "Caused by");
    }
}
