package com.videoteca.frontend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoteca.frontend.dto.Pelicula;
import com.videoteca.frontend.exception.GatewayErrorAdvice;
import java.net.ConnectException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Tests de l'endpoint {@code GET /movies} amb el client HTTP del gateway
 * simulat (cap petició real).
 */
@ExtendWith(MockitoExtension.class)
class MoviesControllerTest {

    private static final Pelicula PELICULA_EXEMPLE = new Pelicula(
            1L, "Forrest Gump", "La vida vista a través dels ulls de Forrest Gump.", 1994, 142, 7L, 42L);

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RestClient gatewayClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MoviesController(gatewayClient))
                .setControllerAdvice(new GatewayErrorAdvice())
                .buildMockMvc();
    }

    /**
     * Quan el gateway respon 200 amb una llista de pel·lícules, el controlador
     * les retorna tal qual (mapejades des del JSON amb camps snake_case).
     */
    @Test
    void gatewayDisponableRetornaLaLlistaDePellicules() throws Exception {
        when(gatewayClient.get().uri("/movies").retrieve().body(any()))
                .thenReturn(List.of(PELICULA_EXEMPLE));

        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Forrest Gump"))
                .andExpect(jsonPath("$.sinopsis").value("La vida vista a través dels ulls de Forrest Gump."))
                .andExpect(jsonPath("$.anio_estreno").value(1994))
                .andExpect(jsonPath("$.duracion_minutos").value(142))
                .andExpect(jsonPath("$.genero_id").value(7))
                .andExpect(jsonPath("$.director_id").value(42));
    }

    /**
     * Quan el gateway no està disponible (connexió refusada, timeout, ...), el
     * controlador retorna 503 amb un JSON descriptiu { "error": ... } i sense
     * cap stack trace a la resposta.
     */
    @Test
    void gatewayNoDisponibleRetorna503AmbJsonError() throws Exception {
        when(gatewayClient.get().uri("/movies").retrieve().body(any()))
                .thenThrow(new ResourceAccessException("No s'ha pogut connectar al gateway",
                        new ConnectException("Connection refused")));

        String body = mockMvc.perform(get("/movies"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(
                        "El servei 'movies' no és disponible: el gateway o el servei de pel·lícules "
                                + "no ha pogut respondre la petició. Intenteu-ho de nou més tard."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Només el camp "error": ni stack trace ni detalls interns a la resposta.
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("java.net")
                .doesNotContain("at ");
    }
}
