package com.videoteca.actors.repository;

import com.videoteca.actors.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA de la entidad {@link Actor}.
 */
public interface ActorRepository extends JpaRepository<Actor, UUID> {
}
