package com.videoteca.actors.service;

import com.videoteca.actors.domain.Actor;
import com.videoteca.actors.domain.ActorNotFoundException;
import com.videoteca.actors.dto.ActorPageResponse;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.repository.ActorRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negoci dels actors: llistat, llistat paginat, cerca per text,
 * consulta per id, creació, actualització completa i eliminació.
 *
 * <p>Aïlla el controlador de la persistència i fa el mapeig
 * entitat &harr; DTO ({@link ActorResponse}). Si un actor no existeix, llança
 * {@link ActorNotFoundException} (que el control d'errors tradueix a un 404);
 * la validació de camps és responsabilitat del controlador (DTO amb
 * Bean Validation), no d'aquest servei.</p>
 */
@Service
@Transactional(readOnly = true)
public class ActorService {

    private final ActorRepository repository;

    public ActorService(ActorRepository repository) {
        this.repository = repository;
    }

    /**
     * Llista tots els actors.
     *
     * @return llista d'actors (potser buida, mai {@code null})
     */
    public List<ActorResponse> listar() {
        return repository.findAll().stream().map(ActorResponse::from).toList();
    }

    /**
     * Llista els actors per pàgines (p. ex. {@code page=0&size=20}).
     *
     * @param pageable paràmetres de paginació i ordenació
     * @return pàgina d'actors amb el metadatum de la consulta
     */
    public ActorPageResponse listarPaginat(Pageable pageable) {
        Page<Actor> pagina = repository.findAll(pageable);
        return new ActorPageResponse(
                pagina.getContent().stream().map(ActorResponse::from).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }

    /**
     * Cerca actors pel nom o els cognoms (indiferent de majúscules i minúscules).
     *
     * @param text fragment de text a buscar
     * @return actors que coincideixen (potser buida, mai {@code null})
     */
    public List<ActorResponse> buscarPerText(String text) {
        return repository
                .findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(text, text)
                .stream()
                .map(ActorResponse::from)
                .toList();
    }

    /**
     * Busca un actor per identificador.
     *
     * @param id identificador de l'actor
     * @return l'actor trobat
     * @throws ActorNotFoundException si no existeix cap actor amb aquest id
     */
    public ActorResponse buscarPerId(Long id) {
        return ActorResponse.from(obtenirEntitat(id));
    }

    /**
     * Crea un actor nou a partir de la petició.
     *
     * @param peticio dades del nou actor
     * @return l'actor creat amb el seu identificador assignat
     */
    @Transactional
    public ActorResponse crear(ActorRequest peticio) {
        Actor actor = new Actor();
        aplicarCamps(actor, peticio);
        return ActorResponse.from(repository.save(actor));
    }

    /**
     * Actualitza tots els camps d'un actor existent.
     *
     * @param id      identificador de l'actor a actualitzar
     * @param peticio nous valors de tots els camps
     * @return l'actor actualitzat
     * @throws ActorNotFoundException si no existeix cap actor amb aquest id
     */
    @Transactional
    public ActorResponse actualizar(Long id, ActorRequest peticio) {
        Actor actor = obtenirEntitat(id);
        aplicarCamps(actor, peticio);
        return ActorResponse.from(repository.save(actor));
    }

    /**
     * Elimina un actor existent.
     *
     * @param id identificador de l'actor a eliminar
     * @throws ActorNotFoundException si no existeix cap actor amb aquest id
     */
    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtenirEntitat(id));
    }

    /**
     * Carga l'entitat persistida o llança l'excepció de domini si no existeix.
     */
    private Actor obtenirEntitat(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ActorNotFoundException(id));
    }

    /**
     * Copia els camps de la petició a l'entitat (creació o actualització).
     */
    private void aplicarCamps(Actor actor, ActorRequest peticio) {
        actor.setNombre(peticio.nombre());
        actor.setApellidos(peticio.apellidos());
        actor.setFechaNacimiento(peticio.fechaNacimiento());
        actor.setNacionalidad(peticio.nacionalidad());
        actor.setFoto(peticio.foto());
        actor.setBiografia(peticio.biografia());
    }
}
