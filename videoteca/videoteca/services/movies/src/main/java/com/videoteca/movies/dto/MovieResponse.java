package com.videoteca.movies.dto;

import com.videoteca.movies.entity.Movie;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para exponer una película en la API.
 */
public record MovieResponse(
        Long id,
        String title,
        String director,
        Integer releaseYear,
        String genre,
        String description,
        BigDecimal rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MovieResponse fromEntity(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getReleaseYear(),
                movie.getGenre(),
                movie.getDescription(),
                movie.getRating(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }
}
