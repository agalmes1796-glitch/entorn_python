package com.videoteca.actors.dto;

import com.videoteca.actors.entity.FilmographyEntry;

/**
 * DTO de salida para una entrada de filmografía expuesta en la API.
 */
public record FilmographyEntryResponse(
        String title,
        Integer year,
        String role
) {

    public static FilmographyEntryResponse fromEntity(FilmographyEntry entry) {
        return new FilmographyEntryResponse(entry.getTitle(), entry.getYear(), entry.getRole());
    }
}
