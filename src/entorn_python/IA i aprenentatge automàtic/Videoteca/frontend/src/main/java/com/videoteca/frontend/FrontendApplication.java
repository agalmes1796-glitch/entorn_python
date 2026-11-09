package com.videoteca.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservici de front del projecte Videoteca.
 *
 * <p>Exposa {@code GET /movies} delegant la consulta al servei "movies"
 * a través del gateway (vegeu {@link MoviesController}). No llegeix la BD
 * directament: consumeix l'endpoint {@code GET {gateway.base-url}/movies},
 * que el gateway enruta al servei "movies" registrat a Eureka mitjançant el
 * discovery locator.</p>
 */
@SpringBootApplication
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
