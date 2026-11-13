package com.videoteca.actors.service;

import com.videoteca.actors.dto.ActorPatchRequest;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.dto.FilmographyEntryRequest;
import com.videoteca.actors.entity.Actor;
import com.videoteca.actors.entity.FilmographyEntry;
import com.videoteca.actors.exception.ActorNotFoundException;
import com.videoteca.actors.repository.ActorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ActorService} (Mockito, sin Spring ni base de datos).
 */
@ExtendWith(MockitoExtension.class)
class ActorServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private ActorRepository repository;

    @InjectMocks
    private ActorService actorService;

    /** Actor existente en el "repositorio" (con id fijo para los tests). */
    private Actor actorExistente() {
        Actor actor = new Actor(
                "Anthony Hopkins",
                LocalDate.of(1937, 12, 31),
                "Portmadog",
                "Leyenda del cine británico.",
                List.of(new FilmographyEntry("El Padrino", 1972, "Michael Corleone")));
        actor.setId(ID);
        return actor;
    }

    private ActorRequest requestCompleto() {
        return new ActorRequest(
                "Meryl Streep",
                LocalDate.of(1949, 6, 22),
                "New York",
                "Actriz estadounidense.",
                List.of(new FilmographyEntryRequest("El resplandor", 1980, "Wendy Torrance")));
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_debePersistirYDevolverElActorCreado() {
        when(repository.save(any(Actor.class)))
                .thenAnswer(invocation -> {
                    Actor actor = invocation.getArgument(0);
                    actor.setId(ID);
                    return actor;
                });

        ActorResponse response = actorService.create(requestCompleto());

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.name()).isEqualTo("Meryl Streep");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1949, 6, 22));
        assertThat(response.filmography()).hasSize(1);
        assertThat(response.filmography().get(0).title()).isEqualTo("El resplandor");
        verify(repository).save(any(Actor.class));
    }

    // ------------------------------------------------------------- findById

    @Test
    void findById_debeDevolverElActorSiExiste() {
        when(repository.findById(ID)).thenReturn(Optional.of(actorExistente()));

        ActorResponse response = actorService.findById(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.name()).isEqualTo("Anthony Hopkins");
        assertThat(response.birthPlace()).isEqualTo("Portmadog");
        assertThat(response.filmography()).hasSize(1);
    }

    @Test
    void findById_debeLanzarActorNotFoundSiNoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actorService.findById(ID))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_debeSustituirCompletamenteLosCampos() {
        when(repository.findById(ID)).thenReturn(Optional.of(actorExistente()));

        // PUT sin birthDate ni filmografía: reemplazo completo (los campos se vacían)
        ActorRequest request = new ActorRequest("Nuevo nombre", null, null, null, null);

        ActorResponse response = actorService.update(ID, request);

        assertThat(response.name()).isEqualTo("Nuevo nombre");
        assertThat(response.birthDate()).isNull();
        assertThat(response.filmography()).isEmpty();
        // La respuesta 200 refleja la modificación real (updatedAt fijado explícitamente)
        assertThat(response.updatedAt()).isNotNull();
        // La persistencia se deja al dirty-checking (no se llama a save)
        verify(repository, never()).save(any());
    }

    @Test
    void update_debeLanzarActorNotFoundSiNoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actorService.update(ID, requestCompleto()))
                .isInstanceOf(ActorNotFoundException.class);
        verify(repository, never()).save(any());
    }

    // ----------------------------------------------------------------- patch

    @Test
    void patch_debeActualizarSoloLosCamposPresentes() {
        when(repository.findById(ID)).thenReturn(Optional.of(actorExistente()));

        ActorPatchRequest patch = new ActorPatchRequest(null, null, "Londres", null, null);

        ActorResponse response = actorService.patch(ID, patch);

        assertThat(response.birthPlace()).isEqualTo("Londres");
        // El resto permanece intacto
        assertThat(response.name()).isEqualTo("Anthony Hopkins");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1937, 12, 31));
        assertThat(response.filmography()).hasSize(1);
        // La respuesta 200 refleja la modificación real (updatedAt fijado explícitamente)
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void patch_debeReemplazarLaFilmografiaSiSeEnvia() {
        when(repository.findById(ID)).thenReturn(Optional.of(actorExistente()));

        ActorPatchRequest patch = new ActorPatchRequest(null, null, null, null,
                List.of(new FilmographyEntryRequest("Silencio del inocente", 1991, "Cannibal Hollow")));

        ActorResponse response = actorService.patch(ID, patch);

        assertThat(response.filmography()).hasSize(1);
        assertThat(response.filmography().get(0).title()).isEqualTo("Silencio del inocente");
        assertThat(response.name()).isEqualTo("Anthony Hopkins"); // sin cambios
    }

    @Test
    void patch_debeLanzarActorNotFoundSiNoExiste() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actorService.patch(ID, new ActorPatchRequest(null, null, null, null, null)))
                .isInstanceOf(ActorNotFoundException.class);
    }

    // ---------------------------------------------------------------- delete

    @Test
    void delete_debeEliminarElActorSiExiste() {
        when(repository.existsById(ID)).thenReturn(true);

        actorService.delete(ID);

        verify(repository).deleteById(ID);
    }

    @Test
    void delete_debeLanzarActorNotFoundSiNoExiste() {
        when(repository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> actorService.delete(ID))
                .isInstanceOf(ActorNotFoundException.class);
        verify(repository, never()).deleteById(any(UUID.class));
    }

    // --------------------------------------------------------------- findAll

    @Test
    void findAll_debeDevolverLaListaDeActores() {
        when(repository.findAll()).thenReturn(List.of(actorExistente()));

        List<ActorResponse> response = actorService.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("Anthony Hopkins");
    }
}
