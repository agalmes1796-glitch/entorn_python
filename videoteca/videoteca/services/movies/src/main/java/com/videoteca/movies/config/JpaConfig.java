package com.videoteca.movies.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuración de la capa de persistencia del microservice.
 *
 * <p>Fija explícitamente el paquete de repositorios Spring Data JPA
 * (las entidades viven en {@code com.videoteca.movies.entity} y son
 * detectadas por el component scan de la clase principal).
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.videoteca.movies.repository")
public class JpaConfig {
}
