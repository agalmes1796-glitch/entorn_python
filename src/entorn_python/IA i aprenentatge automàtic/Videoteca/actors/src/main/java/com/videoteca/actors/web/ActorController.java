package com.videoteca.actors.web;

import com.videoteca.actors.dto.ActorPageResponse;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.service.ActorService;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST d'actors: CRUD complet sota {@code /api/actors}.
 *
 * <ul>
 *   <li>{@code GET /api/actors} → tots els actors (200). Amb els paràmetres
 *       opcionals {@code page} i {@code size} → resposta paginada; amb
 *       {@code texto} → cerca pel nom o els cognoms.</li>
 *   <li>{@code GET /api/actors/{id}} → un actor (200) o 404.</li>
 *   <li>{@code POST /api/actors} → crea un actor (201 + header Location).</li>
 *   <li>{@code PUT /api/actors/{id}} → actualitza tots els camps d'un actor
 *       existent (200) o 404.</li>
 *   <li>{@code DELETE /api/actors/{id}} → elimina un actor existent (204) o 404.</li>
 * </ul>
 *
 * <p>Els errors (400, 404, 500) els tradueix {@code ActorExceptionHandler}
 * al cos d'error estàndard de l'API.</p>
 */
@RestController
@RequestMapping("/api/actors")
public class ActorController {

    /** Mida de pàgina per defecte del llistat paginat. */
    private static final int TAMANY_PAGINA_PER_DEFECTE = 20;

    private final ActorService service;

    public ActorController(ActorService service) {
        this.service = service;
    }

    /**
     * Llista els actors segons els paràmetres opcionals de la petició:
     * sense paràmetres → llista completa (200); amb {@code page}/{@code size}
     * → resposta paginada (200); amb {@code texto} → cerca pel nom o els
     * cognoms (200).
     */
    @GetMapping
    public ResponseEntity<Object> llistar(
            @RequestParam(value = "texto", required = false) String texto,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (texto != null && !texto.isBlank()) {
            List<ActorResponse> resultats = service.buscarPerText(texto);
            return ResponseEntity.ok(resultats);
        }
        if (page != null || size != null) {
            int numeroPagina = page != null ? Math.max(page, 0) : 0;
            int tamanyPagina = size != null ? Math.max(size, 1) : TAMANY_PAGINA_PER_DEFECTE;
            ActorPageResponse pagina = service.listarPaginat(
                    PageRequest.of(numeroPagina, tamanyPagina, Sort.by("id")));
            return ResponseEntity.ok(pagina);
        }
        return ResponseEntity.ok(service.listar());
    }

    /**
     * Retorna un actor per identificador (404 si no existeix).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActorResponse> buscarPerId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPerId(id));
    }

    /**
     * Crea un actor nou i retorna el codi 201 amb el header Location apuntant
     * al recurs creat i el cos de la resposta.
     */
    @PostMapping
    public ResponseEntity<ActorResponse> crear(@Valid @RequestBody ActorRequest peticio) {
        ActorResponse creat = service.crear(peticio);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creat.id())
                .toUri();
        return ResponseEntity.created(location).body(creat);
    }

    /**
     * Actualitza tots els camps d'un actor existent (404 si no existeix).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ActorResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActorRequest peticio) {
        return ResponseEntity.ok(service.actualizar(id, peticio));
    }

    /**
     * Elimina un actor existent (404 si no existeix).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
