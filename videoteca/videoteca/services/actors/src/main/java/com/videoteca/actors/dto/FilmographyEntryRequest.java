package com.videoteca.actors.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para una entrada de filmografía (película asociada a un actor).
 *
 * <p>Compartido entre {@link ActorRequest} (crear/sustituir) y
 * {@link ActorPatchRequest} (actualización parcial).
 */
public record FilmographyEntryRequest(

        @NotBlank(message = "El título de la película es obligatorio")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String title,

        @Min(value = 1900, message = "El año debe ser 1900 o posterior")
        @Max(value = 2030, message = "El año debe ser 2030 o anterior")
        Integer year,

        @Size(max = 120, message = "El papel no puede superar los 120 caracteres")
        String role
) {
}
