package com.videoteca.actors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cos de la petició de creació (POST) o actualització (PUT) d'un actor.
 *
 * <p>{@code nombre} i {@code apellidos} són obligatoris; la resta de camps
 * són opcionals (un valor {@code null} en un PUT vol dir "deixa el camp
 * buit"). El controlador aplica la validació amb {@code @Valid} i un error
 * retorna un 400 amb el detall dels camps afectats.</p>
 *
 * @param nombre           nom de l'actor (obligatori, màxim 100 caràcters)
 * @param apellidos        cognoms de l'actor (obligatoris, màxim 100 caràcters)
 * @param fechaNacimiento  data de naixement (opcional)
 * @param nacionalidad     nacionalitat (opcional, màxim 100 caràcters)
 * @param foto             URL de la foto (opcional, màxim 2048 caràcters)
 * @param biografia        biografia (opcional, màxim 65535 caràcters, columna TEXT)
 */
public record ActorRequest(
        @NotBlank(message = "El nom de l'actor no pot ser buit")
        @Size(max = 100, message = "El nom de l'actor no pot superar els 100 caràcters")
        String nombre,

        @NotBlank(message = "Els cognoms de l'actor no poden ser buits")
        @Size(max = 100, message = "Els cognoms de l'actor no poden superar els 100 caràcters")
        String apellidos,

        LocalDate fechaNacimiento,

        @Size(max = 100, message = "La nacionalitat no pot superar els 100 caràcters")
        String nacionalidad,

        @Size(max = 2048, message = "L'URL de la foto no pot superar els 2048 caràcters")
        String foto,

        @Size(max = 65535, message = "La biografia no pot superar els 65535 caràcters")
        String biografia) {
}
