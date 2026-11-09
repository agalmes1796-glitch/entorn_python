package com.videoteca.actors.repository;

import com.videoteca.actors.domain.Actor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositori Spring Data JPA d'actors.
 *
 * <p>A més de les operacions CRUD bàsiques de {@link JpaRepository}
 * (listar, buscar per id, guardar, eliminar), aporta una consulta
 * derivada de cerca per text sobre el nom o els cognoms.</p>
 */
public interface ActorRepository extends JpaRepository<Actor, Long> {

    /**
     * Cerca actors el nom o els cognoms dels quals contenen el text donat,
     * indiferent de majúscules i minúscules.
     *
     * @param text fragment a buscar al nom
     * @param text fragment a buscar als cognoms
     * @return actors que coincideixen (potser buida, mai {@code null})
     */
    List<Actor> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(String text, String text2);
}
