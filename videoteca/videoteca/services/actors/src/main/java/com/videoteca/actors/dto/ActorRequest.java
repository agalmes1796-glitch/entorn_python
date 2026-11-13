package com.videoteca.actors.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para crear o sustituir un actor (POST / PUT).
 *
 * <p>Separado de la entidad {@link com.videoteca.actors.entity.Actor} y con
 * validación de Jakarta Bean Validation. {@code name} es obligatorio; el resto
 * de campos son opcionales (admiten {@code null}; si se envían, deben cumplir
 * sus restricciones).
 */
public record ActorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        LocalDate birthDate,

        @Size(max = 120, message = "El lugar de nacimiento no puede superar los 120 caracteres")
        String birthPlace,

        @Size(max = 1000, message = "La biografía no puede superar los 1000 caracteres")
        String biography,

        // validación en cascada de las entradas de filmografía
        List<@Valid FilmographyEntryRequest> filmography
) {
}
