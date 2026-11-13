package com.videoteca.actors.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para actualizar parcialmente un actor (PATCH).
 *
 * <p>Todos los campos son opcionales: solo se actualizan los que se envían
 * (distintos de {@code null}). Si un campo se envía, debe cumplir sus
 * restricciones de validación (por eso {@code name} no usa {@code @NotBlank},
 * que rechazará un {@code null} legítimo de "campo no enviado").
 */
public record ActorPatchRequest(

        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        LocalDate birthDate,

        @Size(max = 120, message = "El lugar de nacimiento no puede superar los 120 caracteres")
        String birthPlace,

        @Size(max = 1000, message = "La biografía no puede superar los 1000 caracteres")
        String biography,

        // validación en cascada de las entradas de filmografía (solo si el campo se envía)
        List<@Valid FilmographyEntryRequest> filmography
) {
}
