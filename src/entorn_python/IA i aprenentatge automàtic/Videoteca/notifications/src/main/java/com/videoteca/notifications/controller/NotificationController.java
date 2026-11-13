package com.videoteca.notifications.controller;

import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;
import com.videoteca.notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import java.util.UUID;

/**
 * API REST de notificacions.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET    /api/notifications[?read=...&page=...&size=...]} — llista paginada
 *       (per defecte: pàgina 0, 20 per pàgina, màxim 100, ordre descendent per data de
 *       creació; filtre opcional per estat de lectura)</li>
 *   <li>{@code GET    /api/notifications/{id}} — consulta per id</li>
 *   <li>{@code POST   /api/notifications} — crea (201 + capçalera {@code Location})</li>
 *   <li>{@code PUT    /api/notifications/{id}} — actualitza</li>
 *   <li>{@code DELETE /api/notifications/{id}} — elimina (204)</li>
 *   <li>{@code PATCH  /api/notifications/{id}/read} — marca com a llegida</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    /** Límit de mida de pàgina: mai més de 100 elements per resposta. */
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /**
     * Llista les notificacions paginades, o les filtra per l'estat de lectura indicat.
     *
     * <p>Per defecte: pàgina 0, 20 elements per pàgina i ordre descendent per
     * data de creació. La mida s'acota a {@value #MAX_PAGE_SIZE} elements en el
     * servidor (també reflectida a {@code spring.data.web.pageable.max-page-size}).</p>
     */
    @GetMapping
    public Page<NotificationResponse> getAll(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // S'acoten els paràmetres per evitar índexs de pàgina negatius o mides
        // invàlides (PageRequest exigeix pàgina >= 0 i mida > 0).
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return service.getAll(read, pageable);
    }

    /** Retorna la notificació indicada per l'id (404 si no existeix). */
    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /** Crea una notificació; respon 201 amb la capçalera {@code Location} del recurs nou. */
    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/notifications/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /** Actualitza la notificació indicada per l'id (404 si no existeix, 400 si la petició és invàlida). */
    @PutMapping("/{id}")
    public NotificationResponse update(@PathVariable UUID id, @Valid @RequestBody NotificationRequest request) {
        return service.update(id, request);
    }

    /** Elimina la notificació indicada per l'id; respon 204 sense cos (404 si no existeix). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    /** Marca la notificació com a llegida ({@code read=true}); 404 si no existeix. */
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable UUID id) {
        return service.markAsRead(id);
    }
}
