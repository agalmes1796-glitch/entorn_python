package com.videoteca.actors.web;

import com.videoteca.actors.dto.ActorPatchRequest;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.service.ActorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API REST de actores.
 *
 * <p>El controller solo traduce HTTP ↔ llamadas al {@link ActorService};
 * las reglas viven en la capa de servicio. Un id de path con formato
 * inválido (no UUID) se traduce a 400 por la {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/actors")
public class ActorRestController {

    private final ActorService actorService;

    public ActorRestController(ActorService actorService) {
        this.actorService = actorService;
    }

    /** Lista todos los actores (200). */
    @GetMapping
    public List<ActorResponse> findAll() {
        return actorService.findAll();
    }

    /** Detalle de un actor por id (200 / 404). */
    @GetMapping("/{id}")
    public ActorResponse findById(@PathVariable UUID id) {
        return actorService.findById(id);
    }

    /**
     * Crea un actor (201 + cabecera Location absoluta, p. ej.
     * {@code http://host:8081/api/v1/actors/{id}}), como hace MovieController.
     */
    @PostMapping
    public ResponseEntity<ActorResponse> create(@Valid @RequestBody ActorRequest request) {
        ActorResponse created = actorService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Sustitución completa de un actor (200). */
    @PutMapping("/{id}")
    public ActorResponse update(@PathVariable UUID id,
                                @Valid @RequestBody ActorRequest request) {
        return actorService.update(id, request);
    }

    /** Actualización parcial de un actor (200). */
    @PatchMapping("/{id}")
    public ActorResponse patch(@PathVariable UUID id,
                               @Valid @RequestBody ActorPatchRequest request) {
        return actorService.patch(id, request);
    }

    /** Elimina un actor (204). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        actorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
