package com.videoteca.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservici de front del projecte Videoteca.
 *
 * <p>Exposa l'endpoint {@code GET /movies} però no llegeix la base de dades
 * directament: obté les dades del servei "movies" a través de l'API Gateway
 * (vegeu {@code gateway.base-url} al {@code application.yml}).</p>
 */
@SpringBootApplication
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
