package com.videoteca.actors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Càrrega del context del microservici d'actors amb una MariaDB real
 * (Testcontainers).
 *
 * <p>{@code eureka.client.enabled=false} desactiva el client de Eureka en el
 * test: sense reintents en background contra {@code localhost:8761}
 * (inexistent en l'entorn de test), de manera determinista i ràpida. El
 * conteneur de MariaDB es declara amb {@link ServiceConnection}: Spring Boot
 * el comença i el atura amb el cicle de vida del context i exposa les
 * propietats del datasource (URL, usuari, contrasenya) sense dependre de la
 * docker-compose (no cal el JUnit {@code @Testcontainers}).</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
class ActorsApplicationTests {

    @Test
    void contextLoads() {
        // Que s'obri el context ja implica que JPA ha connectat a la MariaDB
        // del conteneur i ha creat la taula 'actor' (hibernate.ddl-auto=update).
    }

    /**
     * MariaDB real amb la mateixa imatge (11.8) que la docker-compose del projecte.
     *
     * <p>Es declara en un {@code @TestConfiguration} aniat (no directament a la
     * classe de test): en Spring Boot 4 no s'admeten mètodes {@code @Bean} a la
     * classe de prova quan porta anotacions que porten {@code @Import}
     * (com {@code @AutoConfigureMockMvc} en les proves d'integració).</p>
     */
    @TestConfiguration
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        static MariaDBContainer mariadb() {
            return new MariaDBContainer(DockerImageName.parse("mariadb:11.8"))
                    .withDatabaseName("videoteca");
        }
    }
}
