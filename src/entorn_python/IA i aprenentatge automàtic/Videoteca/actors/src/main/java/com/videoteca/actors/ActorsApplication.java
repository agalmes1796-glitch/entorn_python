package com.videoteca.actors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservici d'actors del projecte Videoteca.
 *
 * <p>Exposa un CRUD REST d'actors (vegeu {@code ActorController}) persistit a
 * la base de dades MariaDB del projecte (BD {@code videoteca}, taula
 * {@code actor}). Es registra al servidor Eureka perquè el gateway pugui
 * resoldre'l per descobriment de serveis: les peticions entren pel gateway en
 * la ruta {@code /actors/...} i el discovery locator del gateway les
 * redirigeix automàticament a aquest microservici (el nom de la ruta coincideix
 * amb {@code spring.application.name}; vegeu {@code application.yml}).</p>
 */
@SpringBootApplication
public class ActorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActorsApplication.class, args);
    }
}
