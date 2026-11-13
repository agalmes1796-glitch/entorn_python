package com.videoteca.movies.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o sustituir una película.
 *
 * <p>Separado de la entidad {@link com.videoteca.movies.entity.Movie} y con
 * validación de Jakarta Bean Validation. Los campos opcionales admiten
 * {@code null}; si se envían, deben cumplir sus restricciones de longitud.
 */
public record MovieRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 300, message = "El título no puede superar los 300 caracteres")
        String title,

        @Size(max = 200, message = "El director no puede superar los 200 caracteres")
        String director,

        @NotNull(message = "El año de estreno es obligatorio")
        @Min(value = 1895, message = "El año de estreno debe ser 1895 o posterior")
        @Max(value = 2100, message = "El año de estreno debe ser 2100 o anterior")
        Integer releaseYear,

        @Size(max = 100, message = "El género no puede superar los 100 caracteres")
        String genre,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String description,

        @DecimalMin(value = "0", inclusive = true, message = "La puntuación debe ser >= 0")
        @DecimalMax(value = "10", inclusive = true, message = "La puntuación debe ser <= 10")
        @Digits(integer = 2, fraction = 2, message = "La puntuación debe tener 2 decimales como máximo")
        BigDecimal rating
) {
}
