package com.videoteca.actors.dto;

import com.videoteca.actors.domain.Actor;

import java.time.LocalDate;

/**
 * Resposta d'un actor: l'identificador persistit més tots els camps del
 * registre. És només lectura: s'envia al client en les respostes de l'API
 * (200, 201).
 *
 * @param id               identificador de l'actor a la base de dades
 * @param nombre           nom de l'actor
 * @param apellidos        cognoms de l'actor
 * @param fechaNacimiento  data de naixement (o {@code null} si no consta)
 * @param nacionalidad     nacionalitat (o {@code null})
 * @param foto             URL de la foto (o {@code null})
 * @param biografia        biografia (o {@code null})
 */
public record ActorResponse(
        Long id,
        String nombre,
        String apellidos,
        LocalDate fechaNacimiento,
        String nacionalidad,
        String foto,
        String biografia) {

    /** Construeix la resposta a partir de l'entitat persistida. */
    public static ActorResponse from(Actor actor) {
        return new ActorResponse(
                actor.getId(),
                actor.getNombre(),
                actor.getApellidos(),
                actor.getFechaNacimiento(),
                actor.getNacionalidad(),
                actor.getFoto(),
                actor.getBiografia());
    }
}
