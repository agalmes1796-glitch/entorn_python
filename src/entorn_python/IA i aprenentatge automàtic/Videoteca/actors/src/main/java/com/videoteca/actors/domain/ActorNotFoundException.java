package com.videoteca.actors.domain;

/**
 * Excepció de domini que es llança quan es cerca, actualitza o elimina un
 * actor per identificador i no existeix.
 *
 * <p>El consell {@code ActorExceptionHandler} la tradueix a una resposta HTTP
 * 404 amb el cos d'error estàndard de l'API.</p>
 */
public class ActorNotFoundException extends RuntimeException {

    private final Long actorId;

    public ActorNotFoundException(Long actorId) {
        super("L'actor amb identificador " + actorId + " no existeix");
        this.actorId = actorId;
    }

    public Long getActorId() {
        return actorId;
    }
}
