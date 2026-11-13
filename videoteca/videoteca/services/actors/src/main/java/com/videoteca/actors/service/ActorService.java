package com.videoteca.actors.service;

import com.videoteca.actors.dto.ActorPatchRequest;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.dto.FilmographyEntryRequest;
import com.videoteca.actors.entity.Actor;
import com.videoteca.actors.entity.FilmographyEntry;
import com.videoteca.actors.exception.ActorNotFoundException;
import com.videoteca.actors.repository.ActorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reglas de negocio de actores.
 *
 * <p>Capa intermedia entre el {@code web.ActorRestController} (solo HTTP) y el
 * {@code repository} (persistencia).
 */
@Service
public class ActorService {

    private final ActorRepository repository;

    public ActorService(ActorRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista todos los actores.
     */
    @Transactional(readOnly = true)
    public List<ActorResponse> findAll() {
        return repository.findAll().stream()
                .map(ActorResponse::fromEntity)
                .toList();
    }

    /**
     * Detalle de un actor por id.
     *
     * @throws ActorNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public ActorResponse findById(UUID id) {
        return repository.findById(id)
                .map(ActorResponse::fromEntity)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Crea un actor a partir de la petición.
     */
    @Transactional
    public ActorResponse create(ActorRequest request) {
        Actor actor = new Actor(
                request.name(),
                request.birthDate(),
                request.birthPlace(),
                request.biography(),
                toEntities(request.filmography()));
        return ActorResponse.fromEntity(repository.save(actor));
    }

    /**
     * Sustitución completa de un actor existente.
     *
     * <p>Los campos no enviados (null) se vacían, incluida la filmografía.
     *
     * @throws ActorNotFoundException si no existe
     */
    @Transactional
    public ActorResponse update(UUID id, ActorRequest request) {
        Actor actor = repository.findById(id).orElseThrow(() -> notFound(id));
        actor.setName(request.name());
        actor.setBirthDate(request.birthDate());
        actor.setBirthPlace(request.birthPlace());
        actor.setBiography(request.biography());
        actor.setFilmography(toEntities(request.filmography()));
        // Se fija explícitamente para que la respuesta 200 refleje la modificación
        // real; @PreUpdate queda como red de seguridad.
        actor.setUpdatedAt(LocalDateTime.now());
        // Se persiste por dirty-checking al finalizar la transacción.
        return ActorResponse.fromEntity(actor);
    }

    /**
     * Actualización parcial: solo se modifican los campos presentes
     * (distintos de null) en la petición.
     *
     * @throws ActorNotFoundException si no existe
     */
    @Transactional
    public ActorResponse patch(UUID id, ActorPatchRequest request) {
        Actor actor = repository.findById(id).orElseThrow(() -> notFound(id));
        if (request.name() != null) {
            actor.setName(request.name());
        }
        if (request.birthDate() != null) {
            actor.setBirthDate(request.birthDate());
        }
        if (request.birthPlace() != null) {
            actor.setBirthPlace(request.birthPlace());
        }
        if (request.biography() != null) {
            actor.setBiography(request.biography());
        }
        if (request.filmography() != null) {
            actor.setFilmography(toEntities(request.filmography()));
        }
        // Se fija explícitamente para que la respuesta 200 refleje la modificación
        // real; @PreUpdate queda como red de seguridad.
        actor.setUpdatedAt(LocalDateTime.now());
        // Se persiste por dirty-checking al finalizar la transacción.
        return ActorResponse.fromEntity(actor);
    }

    /**
     * Elimina un actor por id.
     *
     * @throws ActorNotFoundException si no existe
     */
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    /**
     * Convierte los DTOs de filmografía en entidades embebidas.
     *
     * <p>Un null se interpreta como "sin filmografía" (en la sustitución
     * completa vacía la colección; en el PATCH solo se invoca si el campo
     * está presente).
     */
    private List<FilmographyEntry> toEntities(List<FilmographyEntryRequest> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> new FilmographyEntry(entry.title(), entry.year(), entry.role()))
                .toList();
    }

    private ActorNotFoundException notFound(UUID id) {
        return new ActorNotFoundException("Actor con id %s no encontrado".formatted(id));
    }
}
