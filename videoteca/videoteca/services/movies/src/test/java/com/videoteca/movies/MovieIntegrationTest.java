package com.videoteca.movies;

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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de la API REST de películas.
 *
 * <p>Arranca la aplicación completa con el perfil "h2" (H2 en memoria, sin
 * Docker) contra un puerto aleatorio, y consume la API con {@link TestRestTemplate}
 * (en Boot 4 vive en el módulo spring-boot-resttestclient).
 *
 * <p>Usa su propia URL de H2 en memoria (ver {@link #fuentesDePropiedades})
 * para no compartir la base de datos con
 * {@link MoviesServiceApplicationTests}: el ddl-auto del perfil h2 es
 * create-drop y un contexto destruiría el esquema del otro si compartieran
 * la misma URL.
 *
 * <p>Los cuerpos de respuesta se interpretan como JSON genérico
 * ({@link JsonNode}) para no acoplar la prueba a un DTO concreto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("h2")
class MovieIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ruta base de la API (convención /api/v1 del proyecto). */
    private static final String BASE = "/api/v1/movies";

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void fuentesDePropiedades(DynamicPropertyRegistry registry) {
        // URL de H2 propia para esta clase: no compartir la BD en memoria
        // con el contexto de MoviesServiceApplicationTests.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:movies_it;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    private static final String CUERPO_CREACION = """
            {
              "title": "El Padrino",
              "director": "Francis Ford Coppola",
              "releaseYear": 1972,
              "genre": "Drama",
              "description": "Saga familiar de la mafia neoyorquina.",
              "rating": 8.50
            }
            """;

    /** Envía el JSON con {@code Content-Type: application/json} explícito. */
    private static HttpEntity<String> entidadJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    /** Parsea el cuerpo (String) de la respuesta a un árbol JSON. */
    private static JsonNode json(ResponseEntity<String> response) throws Exception {
        return JSON.readTree(response.getBody());
    }

    /**
     * Comprueba que un cuerpo de error cumple el formato definido por la API:
     * { timestamp, status, error, path, message, errors? }.
     */
    private static void assertFormatoError(JsonNode body, int estadoEsperado, String pathEsperado) {
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(body.get("status").asInt()).isEqualTo(estadoEsperado);
        assertThat(body.get("error").asText()).isNotBlank();
        assertThat(body.get("path").asText()).isEqualTo(pathEsperado);
        assertThat(body.get("message").asText()).isNotBlank();
    }

    // ------------------------------------------------------------------ CRUD

    @Test
    void flujoCrudCompleto() throws Exception {
        // 1. POST crea (201 + cabecera Location: .../api/v1/movies/{id})
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode creado = json(created);
        String id = creado.get("id").asText();
        assertThat(id).matches("\\d+");
        assertThat(created.getHeaders().getLocation().toString())
                .startsWith("http://")
                .endsWith(BASE + "/" + id);
        assertThat(creado.get("title").asText()).isEqualTo("El Padrino");
        assertThat(creado.get("director").asText()).isEqualTo("Francis Ford Coppola");
        assertThat(creado.get("releaseYear").asInt()).isEqualTo(1972);
        assertThat(creado.get("rating").decimalValue())
                .isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(creado.get("createdAt").isMissingNode()).isFalse();
        assertThat(LocalDateTime.parse(creado.get("createdAt").asText())).isNotNull();

        // 2. La lista (GET /api/v1/movies) contiene la película creada
        ResponseEntity<String> list = restTemplate.getForEntity(BASE, String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode lista = json(list);
        assertThat(lista.isArray()).isTrue();
        boolean contiene = false;
        for (JsonNode movie : lista) {
            if (movie.get("id").asText().equals(id)) {
                contiene = true;
                break;
            }
        }
        assertThat(contiene).isTrue();

        // 3. GET por id (200)
        ResponseEntity<String> found = restTemplate.getForEntity(BASE + "/" + id, String.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode pelicula = json(found);
        assertThat(pelicula.get("title").asText()).isEqualTo("El Padrino");
        assertThat(pelicula.get("genre").asText()).isEqualTo("Drama");
        LocalDateTime updatedAtAntes = LocalDateTime.parse(pelicula.get("updatedAt").asText());

        // 4. PUT: sustitución completa (los campos no enviados se vacían)
        String cuerpoPut = """
                {
                  "title": "El Padrino II",
                  "releaseYear": 1974
                }
                """;
        ResponseEntity<String> put =
                restTemplate.exchange(BASE + "/" + id, HttpMethod.PUT, entidadJson(cuerpoPut), String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode actualizada = json(put);
        assertThat(actualizada.get("id").asText()).isEqualTo(id);
        assertThat(actualizada.get("title").asText()).isEqualTo("El Padrino II");
        assertThat(actualizada.get("releaseYear").asInt()).isEqualTo(1974);
        assertThat(actualizada.get("director").isNull()).isTrue(); // no enviado → vaciado
        assertThat(actualizada.get("genre").isNull()).isTrue();
        assertThat(actualizada.get("rating").isNull()).isTrue();
        assertThat(LocalDateTime.parse(actualizada.get("updatedAt").asText()))
                .isAfterOrEqualTo(updatedAtAntes);

        // 5. GET posterior: los cambios persisten
        ResponseEntity<String> afterPut = restTemplate.getForEntity(BASE + "/" + id, String.class);
        JsonNode trasPut = json(afterPut);
        assertThat(trasPut.get("title").asText()).isEqualTo("El Padrino II");
        // La auditoría de actualización debe reflejarse de verdad en la BD:
        // updatedAt estrictamente posterior al valor capturado antes del PUT.
        assertThat(LocalDateTime.parse(trasPut.get("updatedAt").asText()))
                .isAfter(updatedAtAntes);

        // 6. DELETE (204)
        ResponseEntity<String> delete =
                restTemplate.exchange(BASE + "/" + id, HttpMethod.DELETE, null, String.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 7. GET posterior al borrado (404)
        ResponseEntity<String> afterDelete = restTemplate.getForEntity(BASE + "/" + id, String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertFormatoError(json(afterDelete), 404, BASE + "/" + id);
    }

    // --------------------------------------------------------------- búsqueda

    @Test
    void listarConSearch_debeFiltrarPorTituloCaseInsensitiveYConEscapadoSeguro() throws Exception {
        // Crea una película con un título singular para no interferir con otros tests
        ResponseEntity<String> created = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        { "title": "Cazafantasmas", "releaseYear": 1984 }
                        """), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Búsqueda parcial, case-insensitive (la query va en minúsculas)
        ResponseEntity<String> baja = restTemplate.getForEntity(
                BASE + "?search=" + URLEncoder.encode("cazafant", StandardCharsets.UTF_8), String.class);
        assertThat(baja.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode coincidenciasBajas = json(baja);
        int totalBajas = 0;
        boolean contiene = false;
        for (JsonNode movie : coincidenciasBajas) {
            totalBajas++;
            if (movie.get("title").asText().equals("Cazafantasmas")) {
                contiene = true;
            }
        }
        assertThat(contiene).isTrue();

        // Búsqueda en mayúsculas: igual de coincidencias (case-insensitive)
        ResponseEntity<String> mayus = restTemplate.getForEntity(
                BASE + "?search=" + URLEncoder.encode("CAZA", StandardCharsets.UTF_8), String.class);
        assertThat(json(mayus).size()).isEqualTo(totalBajas);

        // Comodines LIKE en el input deben tratarse como texto literal:
        // "%" y "_" no deben funcionar como comodines
        assertThat(json(restTemplate.getForEntity(
                BASE + "?search=" + URLEncoder.encode("%", StandardCharsets.UTF_8), String.class)).size()).isZero();
        assertThat(json(restTemplate.getForEntity(
                BASE + "?search=" + URLEncoder.encode("_", StandardCharsets.UTF_8), String.class)).size()).isZero();
    }

    // -------------------------------------------------------------- validación

    @Test
    void crearConTituloVacio_debeDevolver400ConErrorDeCampo() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson("{ \"title\": \"\" }"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = json(response);
        assertFormatoError(body, 400, BASE);
        assertThat(body.get("errors").get("title").asText()).contains("obligatorio");
    }

    @Test
    void crearConAnioFueraDeRango_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        { "title": "Cine prehistórico", "releaseYear": 1890 }
                        """), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("errors").get("releaseYear").asText()).contains("1895");
    }

    @Test
    void crearConPuntuacionFueraDeRango_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        { "title": "Nota exagerada", "releaseYear": 2020, "rating": 11 }
                        """), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("errors").get("rating").asText()).contains("10");
    }

    @Test
    void crearConTituloMuyLargo_debeDevolver400() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                entidadJson("""
                        { "title": "%s", "releaseYear": 1990 }
                        """.formatted("T".repeat(301))), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).get("errors").get("title").asText()).contains("300");
    }

    @Test
    void actualizarConPuntuacionInvalida_debeDevolver400() throws Exception {
        ResponseEntity<String> created =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson(CUERPO_CREACION), String.class);
        String id = json(created).get("id").asText();

        ResponseEntity<String> response = restTemplate.exchange(BASE + "/" + id, HttpMethod.PUT,
                entidadJson("""
                        { "title": "Reparto ajustado", "releaseYear": 1972, "rating": -1 }
                        """), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = json(response);
        assertFormatoError(body, 400, BASE + "/" + id);
        assertThat(body.get("errors").get("rating")).isNotNull();
    }

    @Test
    void crearConCuerpoMalformado_debeDevolver400() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE, HttpMethod.POST, entidadJson("{ \"title\": "), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = json(response);
        assertFormatoError(body, 400, BASE);
        assertThat(body.get("message").asText()).isNotBlank();
    }

    // --------------------------------------------------------------- no-404/400

    @Test
    void obtenerDetalleInexistente_debeDevolver404() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE + "/424242", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = json(response);
        assertFormatoError(body, 404, BASE + "/424242");
        assertThat(body.get("message").asText()).contains("424242");
    }

    @Test
    void actualizarInexistente_debeDevolver404() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(BASE + "/424242", HttpMethod.PUT,
                entidadJson(CUERPO_CREACION), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(response).get("message").asText()).contains("424242");
    }

    @Test
    void borrarInexistente_debeDevolver404() throws Exception {
        ResponseEntity<String> response =
                restTemplate.exchange(BASE + "/424242", HttpMethod.DELETE, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(response).get("message").asText()).contains("424242");
    }

    @Test
    void idNoNumericoEnPath_debeDevolver400() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity(BASE + "/no-es-un-numero", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = json(response);
        assertFormatoError(body, 400, BASE + "/no-es-un-numero");
    }

    @Test
    void crearConContentTypeNoSoportado_debeDevolver415() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<String> response = restTemplate.exchange(BASE, HttpMethod.POST,
                new HttpEntity<>("{\"title\": \"Película\", \"releaseYear\": 1990}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        JsonNode body = json(response);
        assertFormatoError(body, 415, BASE);
        assertThat(body.get("message").asText()).contains("text/plain");
    }

    @Test
    void rutaDesconocida_debeDevolver404() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/noexiste", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- actuator

    @Test
    void actuatorHealth_debeResponder200Up() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).get("status").asText()).isEqualTo("UP");
    }
}
