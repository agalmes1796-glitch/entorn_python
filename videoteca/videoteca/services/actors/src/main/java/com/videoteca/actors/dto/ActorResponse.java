package com.videoteca.actors.dto;

import com.videoteca.actors.entity.Actor;
import com.videoteca.actors.entity.FilmographyEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida para exponer un actor en la API.
 */
public record ActorResponse(
        UUID id,
        String name,
        LocalDate birthDate,
        String birthPlace,
        String biography,
        List<FilmographyEntryResponse> filmography,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ActorResponse fromEntity(Actor actor) {
        List<FilmographyEntryResponse> filmography = actor.getFilmography() == null
                ? List.of()
                : actor.getFilmography().stream()
                        .map(FilmographyEntryResponse::fromEntity)
                        .toList();
        return new ActorResponse(
                actor.getId(),
                actor.getName(),
                actor.getBirthDate(),
                actor.getBirthPlace(),
                actor.getBiography(),
                filmography,
                actor.getCreatedAt(),
                actor.getUpdatedAt()
        );
    }
}
