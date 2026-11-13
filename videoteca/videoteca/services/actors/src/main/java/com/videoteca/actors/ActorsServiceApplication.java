package com.videoteca.actors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio "Actors" (proyecto videoteca).
 */
@SpringBootApplication
public class ActorsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActorsServiceApplication.class, args);
    }
}
