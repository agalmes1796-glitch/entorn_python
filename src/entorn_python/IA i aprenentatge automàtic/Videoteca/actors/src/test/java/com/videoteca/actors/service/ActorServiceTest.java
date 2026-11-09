package com.videoteca.actors.service;

import com.videoteca.actors.domain.Actor;
import com.videoteca.actors.domain.ActorNotFoundException;
import com.videoteca.actors.dto.ActorRequest;
import com.videoteca.actors.dto.ActorResponse;
import com.videoteca.actors.repository.ActorRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves unitàries del {@link ActorService} amb el repositori mockejat
 * (Mockito): sense base de dades ni context de Spring, només la lògica de
 * negoci (mapeig entitat &harr; DTO i excepcions de domini).
 */
@ExtendWith(MockitoExtension.class)
class ActorServiceTest {

    @Mock
    private ActorRepository repository;

    @InjectMocks
    private ActorService service;

    @Test
    void crearGuardaLEntitaITornaLaResposta() {
        when(repository.save(any(Actor.class))).thenAnswer(invocacio -> invocacio.getArgument(0));

        ActorResponse resposta = service.crear(new ActorRequest(
                "Núria", "Figueres", LocalDate.of(1939, 2, 21), "catalana",
                "https://exemple.com/nuria.jpg", "Actriu i dramaturga."));

        assertThat(resposta.nombre()).isEqualTo("Núria");
        assertThat(resposta.apellidos()).isEqualTo("Figueres");
        assertThat(resposta.fechaNacimiento()).isEqualTo(LocalDate.of(1939, 2, 21));
        assertThat(resposta.id()).isNull();
        verify(repository).save(argThat(actor ->
                "Núria".equals(actor.getNombre())
                        && "Figueres".equals(actor.getApellidos())
                        && "catalana".equals(actor.getNacionalidad())));
    }

    @Test
    void buscarPerIdRetornaLaRespostaMapejada() {
        Actor actor = new Actor();
        actor.setId(5L);
        actor.setNombre("Penélope");
        actor.setApellidos("Cruz");
        when(repository.findById(5L)).thenReturn(Optional.of(actor));

        ActorResponse resposta = service.buscarPerId(5L);

        assertThat(resposta.id()).isEqualTo(5L);
        assertThat(resposta.nombre()).isEqualTo("Penélope");
        assertThat(resposta.apellidos()).isEqualTo("Cruz");
    }

    @Test
    void buscarPerIdInexistentLancaActorNotFoundException() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerId(42L))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void actualizarCanviaTotsElsCamps() {
        Actor actor = new Actor();
        actor.setId(7L);
        actor.setNombre("Vell");
        actor.setApellidos("Nom");
        when(repository.findById(7L)).thenReturn(Optional.of(actor));
        when(repository.save(any(Actor.class))).thenAnswer(invocacio -> invocacio.getArgument(0));

        ActorResponse resposta = service.actualizar(7L, new ActorRequest(
                "Nou", "Nom", null, "catalana", "https://exemple.com/nou.jpg", "Biografia nova."));

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.nombre()).isEqualTo("Nou");
        assertThat(resposta.fechaNacimiento()).isNull();
        assertThat(resposta.foto()).isEqualTo("https://exemple.com/nou.jpg");
        verify(repository).save(actor);
    }

    @Test
    void actualizarInexistentLancaActorNotFoundException() {
        when(repository.findById(43L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(43L,
                        new ActorRequest("A", "B", null, null, null, null)))
                .isInstanceOf(ActorNotFoundException.class);
        verify(repository, never()).save(any(Actor.class));
    }

    @Test
    void eliminarSuprimeixLEntitaExistente() {
        Actor actor = new Actor();
        actor.setId(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(actor));

        service.eliminar(9L);

        verify(repository).delete(actor);
    }

    @Test
    void eliminarInexistentLancaActorNotFoundException() {
        when(repository.findById(44L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(44L))
                .isInstanceOf(ActorNotFoundException.class);
        verify(repository, never()).delete(any(Actor.class));
    }

    @Test
    void buscarPerTextConsultaNomIApellidsAmbElMateixText() {
        Actor actor = new Actor();
        actor.setId(1L);
        actor.setNombre("Penélope");
        actor.setApellidos("Cruz");
        when(repository.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase("pen", "pen"))
                .thenReturn(List.of(actor));

        List<ActorResponse> resultats = service.buscarPerText("pen");

        assertThat(resultats).hasSize(1);
        assertThat(resultats.get(0).id()).isEqualTo(1L);
        assertThat(resultats.get(0).apellidos()).isEqualTo("Cruz");
    }
}
