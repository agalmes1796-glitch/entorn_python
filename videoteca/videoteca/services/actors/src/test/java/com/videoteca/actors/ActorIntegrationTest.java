package com.videoteca.actors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de la API REST de actores.
 *
 * <p>Arranca la aplicación completa con el perfil "h2" (H2 en memoria, sin
 * Docker) contra un puerto aleatorio, y consume la API con {@link TestRestTemplate}
 * (en Boot 4 vive en el módulo spring-boot-resttestclient).
 *
 * <p>Cada clase de test usa su propia URL de H2 (ver {@link #fuentesDePropiedades})
 * para que los contextos de Spring no compartan la base de datos en memoria:
 * el ddl-auto del perfil h2 es create-drop, y un contexto destruiría el
 * esquema del otro si compartieran la misma URL.
 *
 * <p>Los cuerpos de respuesta se interpretan como JSON genérico
 * ({@link JsonNode}) para no acoplar la prueba a un DTO concreto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("h2")
class ActorIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ruta base de la API (convención /api/v1 del proyecto). */
    private static final String BASE = "/api/v1/actors";

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void fuentesDePropiedades(DynamicPropertyRegistry registry) {
        // URL de H2 propia para esta clase: no compartir la BD en memoria
        // con el contexto de ActorsServiceApplicationTests.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:actors_it;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    private static final String CUERPO_CREACION = """
            {
              "name": "Meryl Streep",
              "birthDate": "1949-06-22",
              "birthPlace": "New York",
              "biography": "Actriz estadounidense",
              "filmography": [
                { "title": "El resplandor", "year": 1980, "role": "Wendy Torrance" }
              ]
            }
            """;

    /** Envía el JSON con {@code Content-Type: application/json} explícito. */
    private static HttpEntity<String> entidadJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    /** Envía el texto con {@code Content-Type: text/plain} explícito. */
    private static HttpEntity<String> entidadTextoPlain(String texto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(texto, headers);
    }

    /** Parsea el cuerpo (String) de la respuesta a un árbol JSON. */
    private static JsonNode json(ResponseEntity<String> response) throws Exception {
        return JSON.readTree(response.getBody());
    }

    private static List<String> nombres(JsonNode array) {
        List<String> names = new ArrayList<>();
        for (JsonNode actor : array) {
            names.add(actor.get("name").asText());
        }
        return names;
    }

    /** Mensajes del objeto "errors" de un ProblemDetail de validación (o vacío). */
    private static List<String> mensajesDeError(JsonNode problem) {
        JsonNode errors = problem.get("errors");
        List<String> messages = new ArrayList<>();
        if (errors != null) {
            for (JsonNode node : errors) {
                messages.add(node.asText());
            }
        }
        return messages;
    }

    @Test
    void flujoCrudCompleto() throws Exception {
        // 1. POST crea (201 + cabecera Location absoluta: scheme://host:port/api/v1/actors/{id})
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode creado = json(created);
        String id = creado.get("id").asText();
        assertThat(UUID.fromString(id)).isNotNull(); // el id devuelto es un UUID bien formado
        assertThat(created.getHeaders().getLocation().toString())
                .startsWith("http://")
                .endsWith(BASE + "/" + id);
        assertThat(creado.get("createdAt").isMissingNode()).isFalse();

        // 2. La lista (GET /api/v1/actors) contiene al actor creado
        ResponseEntity<String> list = restTemplate.getForEntity(BASE, String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode lista = json(list);
        assertThat(lista.isArray()).isTrue();
        assertThat(nombres(lista)).contains("Meryl Streep");

        // 3. GET por id (200) — se captura updatedAt para comprobar que PUT/PATCH lo renuevan
        ResponseEntity<String> found = restTemplate.getForEntity(BASE + "/" + id, String.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode actor = json(found);
        assertThat(actor.get("name").asText()).isEqualTo("Meryl Streep");
        assertThat(actor.get("birthPlace").asText()).isEqualTo("New York");
        assertThat(actor.get("filmography").get(0).get("title").asText()).isEqualTo("El resplandor");
        assertThat(actor.get("filmography").get(0).get("year").asInt()).isEqualTo(1980);
        LocalDateTime updatedAtAntes = LocalDateTime.parse(actor.get("updatedAt").asText());

        // 4. PUT actualiza (reemplazo completo; los campos no enviados se vacían)
        String cuerpoPut = """
                {
                  "name": "M. Streep",
                  "filmography": [ { "title": "Los puentes de Madison", "year": 1995, "role": "Frances" } ]
                }
                """;
        ResponseEntity<String> put =
                restTemplate.exchange(BASE + "/" + id, HttpMethod.PUT, entidadJson(cuerpoPut), String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode actualizado = json(put);
        assertThat(actualizado.get("name").asText()).isEqualTo("M. Streep");
        assertThat(actualizado.get("birthPlace").isNull()).isTrue(); // no enviado → vaciado
        assertThat(actualizado.get("filmography").size()).isEqualTo(1);
        // updatedAt de la respuesta 200 refleja la modificación (>= el valor previo)
        assertThat(LocalDateTime.parse(actualizado.get("updatedAt").asText()))
                .isAfterOrEqualTo(updatedAtAntes);

        // 5. PATCH parcial (solo birthPlace; el resto permanece intacto)
        ResponseEntity<String> patch = restTemplate.exchange(BASE + "/" + id, HttpMethod.PATCH,
                entidadJson("{ \"birthPlace\": \"Springfield\" }"), String.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode parcheado = json(patch);
        assertThat(parcheado.get("birthPlace").asText()).isEqualTo("Springfield");
        assertThat(parcheado.get("name").asText()).isEqualTo("M. Streep");
        // updatedAt de la respuesta 200 refleja la modificación (>= el valor previo)
        assertThat(LocalDateTime.parse(parcheado.get("updatedAt").asText()))
                .isAfterOrEqualTo(updatedAtAntes);

        // 6. DELETE (204)
        ResponseEntity<String> delete =
                restTemplate.exchange(BASE + "/" + id, HttpMethod.DELETE, null, String.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 7. GET posterior al borrado (404)
        ResponseEntity<String> afterDelete = restTemplate.getForEntity(BASE + "/" + id, String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crearConNombreVacio_debeDevolver400() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson("{ \"name\": \"\" }"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("errors").get("name").asText())
                .contains("obligatorio");
    }

    @Test
    void crearConAnioFueraDeRango_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        {
                          "name": "Actor Sin Limites",
                          "filmography": [ { "title": "Cine primitivo", "year": 1899 } ]
                        }
                        """), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("errors")).isNotNull();
    }

    @Test
    void idMalformadoEnPath_debeDevolver400() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity(BASE + "/no-es-un-uuid", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------- cuerpos malformados (M1)

    @Test
    void crearConCuerpoMalformado_debeDevolver400ProblemJson() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson("{ \"name\": "), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // RFC 9457: content-type problem+json y detalle genérico (no el stack de Jackson)
        assertThat(response.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");
        assertThat(json(response).get("detail").asText()).isNotBlank();
    }

    @Test
    void crearConFechaNacimientoInvalida_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("{ \"name\": \"Actor Malformado\", \"birthDate\": \"abc\" }"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("detail")).isNotNull();
    }

    @Test
    void crearConContentTipoNoSoportado_debeDevolver415() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadTextoPlain(CUERPO_CREACION), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(json(response).get("detail").asText()).contains("text/plain");
    }

    // ------------------------------------------------- tope de longitud de campos

    @Test
    void crearConTituloFilmografiaMuyLargo_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        {
                          "name": "Actor de Titulón",
                          "filmography": [ { "title": "%s", "year": 1990 } ]
                        }
                        """.formatted("T".repeat(201))), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensajesDeError(json(response)))
                .anySatisfy(mensaje -> assertThat(mensaje).contains("200"));
    }

    @Test
    void patchConNombreMuyLargo_debeDevolver400() throws Exception {
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        String id = json(created).get("id").asText();

        ResponseEntity<String> response = restTemplate.exchange(BASE + "/" + id, HttpMethod.PATCH,
                entidadJson("{ \"name\": \"%s\" }".formatted("A".repeat(121))), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensajesDeError(json(response)))
                .anySatisfy(mensaje -> assertThat(mensaje).contains("120"));
    }

    // ------------------------------------------------------- PUT/PATCH de borde

    @Test
    void patchVacio_debeDevolver200YSinCambios() throws Exception {
        // 1. Crea un actor con estado inicial conocido
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = json(created).get("id").asText();

        // 2. PATCH sin ningún campo: 200
        ResponseEntity<String> patch = restTemplate.exchange(BASE + "/" + id, HttpMethod.PATCH,
                entidadJson("{}"), String.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. GET: el recurso permanece intacto
        ResponseEntity<String> found = restTemplate.getForEntity(BASE + "/" + id, String.class);
        JsonNode actor = json(found);
        assertThat(actor.get("name").asText()).isEqualTo("Meryl Streep");
        assertThat(actor.get("birthPlace").asText()).isEqualTo("New York");
        assertThat(actor.get("biography").asText()).isEqualTo("Actriz estadounidense");
        assertThat(actor.get("filmography").size()).isEqualTo(1);
        assertThat(actor.get("filmography").get(0).get("title").asText()).isEqualTo("El resplandor");
    }

    @Test
    void putConFilmografiaVacia_debeVaciarLaFilmografia() throws Exception {
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        String id = json(created).get("id").asText();

        // PUT con filmografía explícitamente vacía (no "no enviada"): la vacía
        ResponseEntity<String> put = restTemplate.exchange(BASE + "/" + id, HttpMethod.PUT,
                entidadJson("{ \"name\": \"M. Streep\", \"filmography\": [] }"), String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(put).get("filmography").size()).isEqualTo(0);

        // GET posterior: la filmografía sigue vacía
        ResponseEntity<String> found = restTemplate.getForEntity(BASE + "/" + id, String.class);
        JsonNode actor = json(found);
        assertThat(actor.get("name").asText()).isEqualTo("M. Streep");
        assertThat(actor.get("filmography").size()).isEqualTo(0);
    }

    // ------------------------------------------------------- rutas no mapeadas

    @Test
    void rutaDesconocida_debeDevolver404ProblemJson() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/noexiste", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");
        assertThat(json(response).get("detail").asText()).isNotBlank();
    }

    @Test
    void rutaAntiguaSinV1_debeDevolver404ProblemJson() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/actors", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");
        assertThat(json(response).get("detail").asText()).contains("api/actors");
    }
}
