package com.videoteca.frontend;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint REST del microservici de front.
 *
 * <p>{@code GET /movies}: retorna la llista de pel·lícules (JSON amb els mateixos
 * camps que la taula {@code pelicula}). La consulta <b>no</b> va a la BD: va al
 * gateway ({@code GET {gateway.base-url}/movies}), que l'enruta al servei
 * "movies" registrat a Eureka. Si el gateway (o el servei "movies") no està
 * disponible, es respon {@code 503 Service Unavailable} amb un JSON
 * {@code {"error": "..."}} descriptiu, sense fugar stack traces ni llançar
 * un 500 genèric.</p>
 */
@RestController
@RequestMapping("/movies")
class MoviesController {

    private final MoviesClient moviesClient;

    MoviesController(MoviesClient moviesClient) {
        this.moviesClient = moviesClient;
    }

    @GetMapping
    public ResponseEntity<?> movies() {
        try {
            List<Pelicula> pelicules = moviesClient.fetchMovies();
            return ResponseEntity.ok(pelicules);
        } catch (MoviesUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
