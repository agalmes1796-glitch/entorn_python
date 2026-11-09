package com.videoteca.frontend;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client HTTP del microservici de front.
 *
 * <p>Obté la llista de pel·lícules demanant al <b>gateway</b>
 * ({@code GET {gateway.base-url}/movies}) en lloc de llegir la BD directament:
 * el gateway enruta la petició al servei "movies" registrat a Eureka mitjançant
 * el discovery locator. La URL base la proporciona el bean de
 * {@link RestClient} (vegeu {@link RestClientConfig}); aquí només s'hi suma la
 * ruta {@code /movies}.</p>
 */
@Component
class MoviesClient {

    private static final ParameterizedTypeReference<List<Pelicula>> LLISTA_PELELICLES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    MoviesClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Demana la llista de pel·lícules al gateway.
     *
     * @throws MoviesUnavailableException si el gateway no pot donar la llista
     *         (no reachable, timeout o resposta d'error).
     */
    List<Pelicula> fetchMovies() {
        try {
            return restClient.get()
                    .uri("/movies")
                    .retrieve()
                    .body(LLISTA_PELELICLES);
        } catch (RestClientResponseException e) {
            // El gateway (o el servei "movies" darrere seu) ha tornat un codi
            // d'error (p. ex. 503 si "movies" no està disponible). Només s'exposa
            // l'estat: el cos de la resposta podria contenre stack traces aliens.
            throw new MoviesUnavailableException(
                    "El gateway ha tornat l'error HTTP " + e.getStatusCode()
                            + " en consultar la llista de pel·lícules.", e);
        } catch (RestClientException e) {
            // Falles de transport: connexió rebutjada, timeout de connexió o
            // de lectura, DNS, ... El missatge inclou l'URL complet.
            throw new MoviesUnavailableException(
                    "No s'ha pogut contactar amb el gateway per consultar les pel·lícules: "
                            + e.getMessage(), e);
        }
    }
}
