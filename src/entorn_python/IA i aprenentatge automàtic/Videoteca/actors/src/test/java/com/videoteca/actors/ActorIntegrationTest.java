package com.videoteca.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoteca.actors.dto.ActorRequest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves d'integració del controlador REST d'actors: MockMvc complet contra
 * una MariaDB real (Testcontainers) amb el context de l'aplicació.
 *
 * <p>Cobreix el flux CRUD complet (crear → 201, llistar → apareix, buscar per
 * id → 200, actualitzar → canvia, eliminar → 204 i després buscar → 404), la
 * validació del cos (400) i el tractament d'actors inexistents (404). El
 * client de Eureka es desactiva perquè les proves no dependin de la xarxa de
 * Docker ni del registre de serveis.</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class ActorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Flux CRUD complet: crea, llista, busca, actualitza, elimina. */
    @Test
    void fluxCrudComplet() throws Exception {
        // 1. Crear → 201 + header Location + cos amb el nou id.
        String cosCreacio = mockMvc.perform(post("/api/actors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "Antonio", "Banderas", LocalDate.of(1955, 8, 10), "espanyola",
                                "https://exemple.com/fotos/antonio.jpg", "Actor i director espanyol."))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/actors/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Antonio"))
                .andExpect(jsonPath("$.apellidos").value("Banderas"))
                .andExpect(jsonPath("$.nacionalidad").value("espanyola"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(cosCreacio).get("id").asLong();

        // 2. Llistar → l'actor creat apareix.
        mockMvc.perform(get("/api/actors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].nombre").value(hasItem("Antonio")));

        // 3. Cercar per text → coincideix (el cognom conté "bande").
        mockMvc.perform(get("/api/actors").param("texto", "bande"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // 4. Get per id → 200 amb les dades persistides.
        mockMvc.perform(get("/api/actors/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Antonio"))
                .andExpect(jsonPath("$.apellidos").value("Banderas"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1955-08-10"));

        // 5. Actualitzar → 200 i els canvis es persisteixen.
        mockMvc.perform(put("/api/actors/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "Antonio", "B.", LocalDate.of(1956, 1, 1), "espanyola",
                                "https://exemple.com/fotos/antonio2.jpg", "Biografia actualitzada."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Antonio"))
                .andExpect(jsonPath("$.apellidos").value("B."))
                .andExpect(jsonPath("$.fechaNacimiento").value("1956-01-01"))
                .andExpect(jsonPath("$.biografia").value("Biografia actualitzada."));

        // 6. Eliminar → 204.
        mockMvc.perform(delete("/api/actors/" + id))
                .andExpect(status().isNoContent());

        // 7. Get després d'eliminar → 404 amb el cos d'error estàndard.
        mockMvc.perform(get("/api/actors/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("no existeix")));
    }

    /** Creació amb nom en blanc → 400 amb el detall del camp no vàlid. */
    @Test
    void creacioAmbNomBuitRetorna400() throws Exception {
        mockMvc.perform(post("/api/actors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "   ", "Dalmau", null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/actors"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'nombre')]").isArray());
    }

    /** Get d'un actor que no existeix → 404 amb el cos d'error estàndard. */
    @Test
    void getInexistentRetorna404() throws Exception {
        mockMvc.perform(get("/api/actors/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/actors/999999"));
    }

    /** Actualitzar i eliminar un actor que no existeix → 404. */
    @Test
    void actualitzarIEliminarInexistentsRetornen404() throws Exception {
        mockMvc.perform(put("/api/actors/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "A", "B", null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/actors/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    /** Llistat paginat: page i size sobre el mateix endpoint. */
    @Test
    void llistatPaginat() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/actors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActorRequest(
                                    "Pag", "Actor" + i, null, null, null, null))))
                    .andExpect(status().isCreated());
        }

        // S'assegura amb "mínim 3" perquè altres proves d'aquesta classe poden
        // haver deixat actors a la BD (el context, i el conteneur, són compartits).
        mockMvc.perform(get("/api/actors").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(2)));
    }

    /** Cerca per text: coincideix pel nom i pels cognoms (indiferent de majuscles). */
    @Test
    void cercaPerTextTrobaNomIApellids() throws Exception {
        String cos = mockMvc.perform(post("/api/actors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "Quimet", "Pi de les Aves", null, "catalana", null, null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(cos).get("id").asLong();

        // Coincideix pel nom ("quim" dins de "Quimet").
        mockMvc.perform(get("/api/actors").param("texto", "quim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].apellidos").value(hasItem("Pi de les Aves")));

        // Coincideix pels cognoms, indiferent de majúscules ("PI DE LES").
        mockMvc.perform(get("/api/actors").param("texto", "PI DE LES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].nombre").value(hasItem("Quimet")));

        // Text sense coincidències → array buit.
        mockMvc.perform(get("/api/actors").param("texto", "zzzznoexisteixzzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * MariaDB real amb la mateixa imatge (11.8) que la docker-compose del projecte.
     *
     * <p>Es declara en un {@code @TestConfiguration} aniat (no directament a la
     * classe de test): en Spring Boot 4 no s'admeten mètodes {@code @Bean} a la
     * classe de prova quan porta anotacions que porten {@code @Import} (com
     * {@code @AutoConfigureMockMvc}). Spring Boot comença i atura el conteneur
     * amb el cicle de vida del context ({@code @ServiceConnection}) i exposa
     * les propietats del datasource, sense dependre de la docker-compose.</p>
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
