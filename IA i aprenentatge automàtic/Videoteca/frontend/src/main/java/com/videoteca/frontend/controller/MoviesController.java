package com.videoteca.frontend.controller;

import com.videoteca.frontend.dto.Pelicula;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Endpoint de pel·lícules del microservici de front.
 *
 * <p>La dada NO llegeix la base de dades directament: fa una petició HTTP al
 * gateway ({@code GET {gateway.base-url}/movies}) i el gateway, gràcies al
 * discovery locator, redirigeix la crida al servei "movies" registrat a Eureka.</p>
 */
@RestController
public class MoviesController {

    private final RestClient gatewayClient;

    public MoviesController(RestClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    /**
     * Llista de pel·lícules obtinguda del servei "movies" via el gateway.
     *
     * <p>Si el gateway o el servei no estan disponibles, el client HTTP llança una
     * {@link org.springframework.web.client.RestClientException} que
     * {@link com.videoteca.frontend.exception.GatewayErrorAdvice} converteix en un
     * 503 amb un cos JSON descriptiu.</p>
     */
    @GetMapping("/movies")
    public List<Pelicula> movies() {
        return gatewayClient.get()
                .uri("/movies")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Pelicula>>() { });
    }
}
