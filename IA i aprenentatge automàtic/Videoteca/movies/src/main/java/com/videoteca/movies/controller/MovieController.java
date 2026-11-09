package com.videoteca.movies.controller;

import com.videoteca.movies.dto.MovieRequest;
import com.videoteca.movies.dto.MovieResponse;
import com.videoteca.movies.service.MovieService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API REST de películas, expuesta bajo DOS bases de URL equivalentes:
 *
 * <pre>
 * Base "" (contrato gateway/frontend):
 * GET    /                   → listado completo
 * GET    /{id}               → una película
 * POST   /                   → crear (201 + Location)
 * PUT    /{id}               → actualizar
 * DELETE /{id}               → eliminar (204)
 * GET    /search?titulo=     → búsqueda por título (LIKE, case-insensitive)
 *
 * Base "/api/movies" (contrato de uso directo, documentado):
 * GET    /api/movies                 → listado completo
 * GET    /api/movies/{id}            → una película
 * POST   /api/movies                 → crear (201 + Location)
 * PUT    /api/movies/{id}            → actualizar
 * DELETE /api/movies/{id}            → eliminar (204)
 * GET    /api/movies/search?titulo=  → búsqueda por título (LIKE, case-insensitive)
 * </pre>
 *
 * <p><b>¿Por qué también en ruta raiz?</b> El gateway (Spring Cloud Gateway 5.x)
 * enruta {@code /<servei>/**} → {@code lb://<servei>/**} con StripPrefix=1
 * (discovery locator, {@code lower-case-service-id: true}): cuando el
 * frontend llama a {@code GET {gateway}/movies} el gateway lo reenvía al
 * servicio {@code movies} como {@code GET /} (path vacío). El DTO del
 * frontend además declara {@code @JsonProperty} en snake_case, por lo que el
 * JSON de este microservicio va en snake_case global (ver
 * {@code spring.jackson.property-naming-strategy} en {@code application.yml}).
 *
 * <p><b>Mecanismo de doble mapeo (verificado en Spring Framework 7.0.10):</b>
 * un único {@code @RequestMapping({"/", "/api/movies"})} a nivel de clase se
 * compone con las sub-rutas de cada método ({@code ""}, {@code "/{id}"},
 * {@code "/search"}) registrando patrones distintos para cada base
 * ({@code /}, {@code /{id}}, {@code /search}, {@code /api/movies},
 * {@code /api/movies/{id}}, {@code /api/movies/search}), sin colisiones:
 * {@code GET /} no choca con {@code GET /{id}} (patrones raiz vs.
 * paramétrico) y el literal {@code /search} mantiene su prioridad sobre el
 * {@code /{id}} paramétrico.
 *
 * <p><b>Nota importante (Spring 7): la base raiz debe ser {@code "/"} y NO
 * {@code ""}.</b> Probado con la versión exacta del framework (7.0.10):
 * {@code PathPatternParser.parse("").matches(PathContainer.parsePath("/"))}
 * devuelve {@code false}, mientras que {@code parse("/").matches(...)}
 * devuelve {@code true}. Con {@code @RequestMapping({"", ...})} el patrón
 * raiz queda registrado como {@code ""} y {@code POST /} / {@code GET /} no
 * encajan en ningún handler (caen en {@code NoResourceFoundException} → 500
 * por el catch-all). Con {@code {"/", ...}} las dos bases funcionan de
 * verdad, como lo cubre la suite de integración
 * ({@code MovieControllerIntegrationTest}).
 *
 * <p><b>Cabecera {@code Location} de POST:</b> se construye con
 * {@link ServletUriComponentsBuilder#fromCurrentRequest()}, de modo que es
 * coherente con la base usada en la petición:
 * <ul>
 *   <li>{@code POST /} → {@code Location: http://<host>/{id}}</li>
 *   <li>{@code POST /api/movies} → {@code Location: http://<host>/api/movies/{id}}</li>
 * </ul>
 *
 * <p><b>Efecto colateral documentado de la base raiz:</b> cualquier petición
 * mono-segmento sobre raiz (p. ej. {@code GET /api} sin {@code /movies},
 * {@code GET /favicon.ico}) ahora encaja en {@code GET /{id}} y devuelve 400
 * (id no numérico) en vez de 404; las rutas de 2+ segmentos
 * ({@code /actuator/health}, {@code /api/movies}) no se ven afectadas.
 */
@RestController
@RequestMapping({"/", "/api/movies"})
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieResponse> findAll() {
        return movieService.findAll();
    }

    @GetMapping("/{id}")
    public MovieResponse findById(@PathVariable Long id) {
        return movieService.findById(id);
    }

    @PostMapping
    public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Búsqueda por título. {@code @NotBlank} sobre el {@code @RequestParam}:
     * un término vacío da 400 (el {@code GlobalExceptionHandler} traduce la
     * {@code MethodValidationException} de Spring 7) en vez de lanzar
     * {@code LIKE '%%'} que devolvería todo el listado.
     */
    @GetMapping("/search")
    public List<MovieResponse> search(
            @RequestParam("titulo")
            @NotBlank(message = "El término de búsqueda (titulo) no puede ser vacío") String titulo) {
        return movieService.findByTituloContainingIgnoreCase(titulo);
    }
}
