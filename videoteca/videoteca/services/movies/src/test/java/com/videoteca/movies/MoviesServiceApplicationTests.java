package com.videoteca.movies;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de arranque: el contexto de la aplicación carga correctamente
 * (perfil "h2", para no depender de MariaDB).
 *
 * <p>Usa su propia URL de H2 en memoria (ver {@link #fuentesDePropiedades})
 * para no compartir la base de datos con {@link MovieIntegrationTest}:
 * el ddl-auto del perfil h2 es create-drop y un contexto destruiría el
 * esquema del otro si compartieran la misma URL.
 */
@SpringBootTest
@ActiveProfiles("h2")
class MoviesServiceApplicationTests {

    @DynamicPropertySource
    static void fuentesDePropiedades(DynamicPropertyRegistry registry) {
        // URL de H2 propia para esta clase.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:movies_ctx;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }
}
