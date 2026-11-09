package com.videoteca.movies.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Prueba de integración del CRUD completo vía MockMvc (sin servidor real),
 * usando H2 en memoria (ver {@code src/test/resources/application.yml}).
 *
 * <p>Cubre: estados HTTP (201+Location, 200, 204, 404, 400, 405, 415), el
 * formato JSON de errores, el mapeo de errores de tipo/método/Content-Type,
 * la validación de rangos de campos opcionales, la búsqueda por título
 * (incluidos términos vacíos y comodines literales) y el flujo completo
 * crear → leer → actualizar → buscar → eliminar → 404.
 *
 * <p><b>Contrato (gateway/frontend):</b> los cuerpos de petición y las
 * respuestas van en snake_case ({@code anio_estreno}, {@code duracion_minutos},
 * {@code genero_id}, {@code director_id}) por la naming strategy global
 * {@code spring.jackson.property-naming-strategy: SNAKE_CASE}; los asertos de
 * errores usan la clave de nivel superior {@code field_errors} (componente del
 * record {@code ApiError}, renomeado por la strategy) pero las claves DEL
 * mapa {@code field_errors} siguen siendo camelCase (nombres de propiedad
 * Java: la strategy no renueva las claves de los {@code Map}).
 *
 * <p>Además se cubre el ciclo completo sobre la <b>ruta raiz</b>
 * ({@code GET /}, {@code POST /} con {@code Location: http://localhost/{id}},
 * {@code GET /{id}}, {@code DELETE /{id}}), que es la que recibe el servicio
 * cuando el gateway hace StripPrefix=1 sobre {@code /movies}.
 *
 * <p>El contexto de test tiene {@code eureka.client.enabled: false} (no hay
 * Eureka en los tests).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MovieControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Ciclo sobre la RUTA RAIZ (contrato gateway/frontend): el gateway
    // reenvía GET {gateway}/movies → GET / (StripPrefix=1).
    // ------------------------------------------------------------------

    @Test
    void cicloEnRutaRaiz_debeCumplirElContratoConElGateway() throws Exception {
        // POST / → 201 Created + cabecera Location exacta sobre la base raiz
        MvcResult created = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Interestelar",
                                  "sinopsis": "Un viaje más allá del sistema solar.",
                                  "anio_estreno": 2014,
                                  "duracion_minutos": 148,
                                  "genero_id": 1,
                                  "director_id": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                // El cuerpo sale en snake_case (naming strategy global)
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Interestelar"))
                .andExpect(jsonPath("$.anio_estreno").value(2014))
                .andExpect(jsonPath("$.duracion_minutos").value(148))
                .andExpect(jsonPath("$.genero_id").value(1))
                .andExpect(jsonPath("$.director_id").value(nullValue()))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = body.get("id").asLong();

        // Location coherente con la base usada: POST / → http://localhost/{id}
        // (host: MockMvc → localhost)
        assertThat(created.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost/" + id);

        // GET / → 200 listado (claves snake_case en el wire)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].titulo", hasItem("Interestelar")))
                .andExpect(jsonPath("$[*].anio_estreno", hasItem(2014)));

        // GET /{id} → 200
        mockMvc.perform(get("/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Interestelar"))
                .andExpect(jsonPath("$.genero_id").value(1))
                .andExpect(jsonPath("$.director_id").value(nullValue()));

        // GET /search?titulo= → 200 (base raiz)
        mockMvc.perform(get("/search").param("titulo", "inter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", hasItem("Interestelar")));

        // DELETE /{id} → 204 sin cuerpo
        mockMvc.perform(delete("/{id}", id))
                .andExpect(status().isNoContent());

        // GET /{id} tras el DELETE → 404 JSON
        mockMvc.perform(get("/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pelicula con id " + id + " no encontrada"));
    }

    // ------------------------------------------------------------------
    // CRUD sobre /api/movies (contrato de uso directo)
    // ------------------------------------------------------------------

    @Test
    void crudCompleto_debeCumplirElFlujoEsperado() throws Exception {
        // POST → 201 Created + cabecera Location exacta (cuerpo en snake_case)
        MvcResult created = mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Interestelar",
                                  "sinopsis": "Un viaje más allá del sistema solar.",
                                  "anio_estreno": 2014,
                                  "duracion_minutos": 148,
                                  "genero_id": 1,
                                  "director_id": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Interestelar"))
                .andExpect(jsonPath("$.anio_estreno").value(2014))
                .andExpect(jsonPath("$.genero_id").value(1))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = body.get("id").asLong();

        // Location apunta exactamente a la recién creada (host: MockMvc → localhost)
        String location = created.getResponse().getHeader("Location");
        assertThat(location)
                .isEqualTo("http://localhost/api/movies/" + id);

        // GET /{id} → 200
        mockMvc.perform(get("/api/movies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Interestelar"));

        // GET listado → incluye la película creada
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].titulo", hasItem("Interestelar")));

        // PUT → 200 con los campos actualizados (cuerpo en snake_case)
        mockMvc.perform(put("/api/movies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Interestelar 2",
                                  "sinopsis": "La secuela.",
                                  "anio_estreno": 2026,
                                  "duracion_minutos": 160,
                                  "genero_id": 2,
                                  "director_id": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Interestelar 2"))
                .andExpect(jsonPath("$.anio_estreno").value(2026))
                .andExpect(jsonPath("$.genero_id").value(2));

        // GET /search?titulo= → LIKE case-insensitive
        mockMvc.perform(get("/api/movies/search").param("titulo", "inter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", hasItem("Interestelar 2")));

        // DELETE → 204 sin cuerpo
        mockMvc.perform(delete("/api/movies/{id}", id))
                .andExpect(status().isNoContent());

        // GET tras el DELETE → 404 JSON (mensaje con concordancia: "no encontrada")
        mockMvc.perform(get("/api/movies/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pelicula con id " + id + " no encontrada"));
    }

    @Test
    void post_sinTituloYGenero_debeDevolver400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sinopsis": "Faltan campos obligatorios."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // "field_errors": componente del record ApiError → snake_case.
                // "titulo"/"generoId": claves DEL mapa → camelCase (nombres de
                // propiedad Java; la strategy no renueva claves de Map).
                .andExpect(jsonPath("$.field_errors.titulo").exists())
                .andExpect(jsonPath("$.field_errors.generoId").exists());
    }

    @Test
    void post_conJsonMalformado_debeDevolver400() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Roto\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void get_inexistente_debeDevolver404Json() throws Exception {
        mockMvc.perform(get("/api/movies/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void delete_inexistente_debeDevolver404Json() throws Exception {
        mockMvc.perform(delete("/api/movies/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void search_sinParametro_debeDevolver400() throws Exception {
        mockMvc.perform(get("/api/movies/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ------------------------------------------------------------------
    // Semántica de errores (M1/M2/M3): tipo de parámetro, método, Content-Type
    // ------------------------------------------------------------------

    @Test
    void get_conIdNoNumerico_debeDevolver400EnLugarDe500() throws Exception {
        mockMvc.perform(get("/api/movies/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Valor inválido para el parámetro 'id': abc (se esperaba Long)"));
    }

    @Test
    void put_conIdNoNumerico_debeDevolver400EnLugarDe500() throws Exception {
        mockMvc.perform(put("/api/movies/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"x\",\"genero_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void delete_conIdNoNumerico_debeDevolver400EnLugarDe500() throws Exception {
        mockMvc.perform(delete("/api/movies/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void patch_inexistente_debeDevolver405() throws Exception {
        mockMvc.perform(patch("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"x\",\"genero_id\":1}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void post_conContentTypeTextPlain_debeDevolver415() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("titulo=Interestelar"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    // ------------------------------------------------------------------
    // Validaciones de rangos en MovieRequest (M1)
    // ------------------------------------------------------------------

    @Test
    void post_conAnioEstrenoFueraDeRango_debeDevolver400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Anio inválido",
                                  "anio_estreno": 99999,
                                  "genero_id": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Clave del mapa field_errors: nombre de propiedad Java (camelCase)
                .andExpect(jsonPath("$.field_errors.anioEstreno")
                        .value("El año de estreno debe estar entre 1000 y 9999"));
    }

    @Test
    void post_conDuracionNegativa_debeDevolver400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Duración inválida",
                                  "duracion_minutos": -5,
                                  "genero_id": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.field_errors.duracionMinutos")
                        .value("La duración no puede ser negativa"));
    }

    @Test
    void post_conAnioEstrenoYDuracionEnRango_debeDevolver201() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Rango válido",
                                  "anio_estreno": 1000,
                                  "duracion_minutos": 65535,
                                  "genero_id": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio_estreno").value(1000))
                .andExpect(jsonPath("$.duracion_minutos").value(65535));
    }

    // ------------------------------------------------------------------
    // Búsqueda: término vacío → 400; comodines literales (B3)
    // ------------------------------------------------------------------

    @Test
    void search_conTituloVacio_debeDevolver400() throws Exception {
        mockMvc.perform(get("/api/movies/search").param("titulo", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.field_errors.titulo")
                        .value("El término de búsqueda (titulo) no puede ser vacío"));
    }

    @Test
    void search_conComodinesLiterales_debeTratarlosComoLiterals() throws Exception {
        // Una película cuyo título contiene '%' literal
        MvcResult created = mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "50% Off",
                                  "genero_id": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // Búsqueda de "50%" debe coincidir SOLO con el '%' literal del título
        // (Spring Data JPA escapa los comodines LIKE en el parámetro), no con
        // un wildcard.
        mockMvc.perform(get("/api/movies/search").param("titulo", "50%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("50% Off"));

        // Buscar "%" no debe devolver el listado completo: solo los títulos
        // que contienen '%' literal.
        mockMvc.perform(get("/api/movies/search").param("titulo", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("50% Off"));

        // Limpieza: dejar el H2 de prueba sin la película de este test
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(delete("/api/movies/{id}", id))
                .andExpect(status().isNoContent());
    }
}
