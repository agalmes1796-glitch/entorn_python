package com.videoteca.notifications;

import com.videoteca.notifications.controller.NotificationController;
import com.videoteca.notifications.dto.NotificationRequest;
import com.videoteca.notifications.dto.NotificationResponse;
import com.videoteca.notifications.exception.NotificationNotFoundException;
import com.videoteca.notifications.model.NotificationType;
import com.videoteca.notifications.service.NotificationService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves d'API del {@link NotificationController} (slice MVC).
 *
 * <p>El {@link NotificationService} es simula amb {@code @MockitoBean}
 * (el substitut de {@code @MockBean} en Spring Boot 4); el gestors
 * d'excepcions ({@code GlobalExceptionHandler}) sí que es carreguen,
 * perquè la capa d'errors forma part del comportament de l'API.</p>
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTests {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService service;

    /** Resposta de mostra amb tots els camps omplerts. */
    private static NotificationResponse sampleResponse(boolean read) {
        return new NotificationResponse(ID, "Títol", "Missatge", NotificationType.SUCCESS,
                read,
                Instant.parse("2026-11-09T10:00:00Z"),
                Instant.parse("2026-11-09T11:00:00Z"));
    }

    // ---------- POST ----------

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(service.create(any(NotificationRequest.class))).thenReturn(sampleResponse(false));

        String result = mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Títol","message":"Missatge","type":"SUCCESS"}
                                """))
                .andExpect(status().isCreated())
                // El Location és una URI absoluta (http://localhost/...) a Boot 4; s'ancora a la part del path.
                .andExpect(header().string("Location", Matchers.containsString("/api/notifications/" + ID)))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.type").value("SUCCESS"))
                .andExpect(jsonPath("$.read").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(result).contains("Títol");
        verify(service).create(any(NotificationRequest.class));
    }

    @Test
    void createDefaultsTypeToInfoWhenMissing() throws Exception {
        when(service.create(any(NotificationRequest.class))).thenReturn(sampleResponse(false));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Títol","message":"Missatge"}
                                """))
                .andExpect(status().isCreated());

        // El tipus per defecte el normalitza el record (no el servei simulat).
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.INFO);
    }

    @Test
    void createReturns400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","message":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.field").isString())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/notifications"));

        verifyNoInteractions(service);
    }

    /** Missatge de 256 caràcters (un més que el màxim) → 400 amb el camp 'message'. */
    @Test
    void createReturns400WhenMessageIsLongerThan255Chars() throws Exception {
        String longMessage = "x".repeat(256);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Títol\",\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.field").value("message"));

        verifyNoInteractions(service);
    }

    /** Missatge de 255 caràcters (exactament el màxim) → 201. */
    @Test
    void createReturns201WhenMessageIsExactly255Chars() throws Exception {
        when(service.create(any(NotificationRequest.class))).thenReturn(sampleResponse(false));
        String message255 = "x".repeat(255);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Títol\",\"message\":\"" + message255 + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(service).create(any(NotificationRequest.class));
    }

    /** JSON malformat (no parsejable) → 400 amb ApiError. */
    @Test
    void createReturns400WhenBodyIsNotValidJson() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Títol\" message\":\"\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/notifications"));

        verifyNoInteractions(service);
    }

    /** Valor del camp 'type' que no és un valor de l'enum → 400 amb ApiError. */
    @Test
    void createReturns400WhenTypeIsNotAValidEnumValue() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Títol","message":"Missatge","type":"NO_EXISTENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(service);
    }

    // ---------- GET ----------

    /** Llistat paginat per defecte: body amb content/totalElements/totalPages/number/size. */
    @Test
    void getAllReturns200WithPage() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(
                List.of(sampleResponse(false), sampleResponse(true)),
                PageRequest.of(0, 20, Sort.by("createdAt").descending()),
                2);
        when(service.getAll(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.content[1].read").value(true))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));

        // El controlador construeix el Pageable per defecte: pàgina 0, mida 20,
        // ordre descendent per createdAt.
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAll(eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    /** El filtre read=true/false es delega al servei (i el body segueix sent Page). */
    @Test
    void getAllPassesTheReadFilterToTheService() throws Exception {
        Page<NotificationResponse> readPage = new PageImpl<>(
                List.of(sampleResponse(true)),
                PageRequest.of(0, 20, Sort.by("createdAt").descending()),
                1);
        Page<NotificationResponse> unreadPage =
                Page.empty(PageRequest.of(0, 20, Sort.by("createdAt").descending()));
        when(service.getAll(eq(true), any(Pageable.class))).thenReturn(readPage);
        when(service.getAll(eq(false), any(Pageable.class))).thenReturn(unreadPage);

        mockMvc.perform(get("/api/notifications").param("read", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/notifications").param("read", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(service).getAll(eq(true), any(Pageable.class));
        verify(service).getAll(eq(false), any(Pageable.class));
    }

    /** ?size=500 s'acota a 100 elements al servidor. */
    @Test
    void getAllCapsTheSizeAtTheServerMaximum() throws Exception {
        when(service.getAll(eq(null), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 100, Sort.by("createdAt").descending())));

        mockMvc.perform(get("/api/notifications").param("page", "0").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAll(eq(null), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(service.getById(ID)).thenReturn(sampleResponse(true));

        mockMvc.perform(get("/api/notifications/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        when(service.getById(ID)).thenThrow(new NotificationNotFoundException(ID));

        mockMvc.perform(get("/api/notifications/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/notifications/" + ID));
    }

    /** id que no és un UUID → 400 amb ApiError i el paràmetre afectat. */
    @Test
    void getByIdReturns400WhenIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/notifications/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.field").value("id"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/notifications/not-a-uuid"));

        verifyNoInteractions(service);
    }

    /** Excepció inesperada del servei → 500 amb missatge genèric. */
    @Test
    void getByIdReturns500WithGenericMessageWhenAnUnexpectedErrorOccurs() throws Exception {
        when(service.getById(ID)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/notifications/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal error"))
                .andExpect(jsonPath("$.path").value("/api/notifications/" + ID));
    }

    // ---------- PUT ----------

    @Test
    void updateReturns200() throws Exception {
        when(service.update(eq(ID), any(NotificationRequest.class))).thenReturn(sampleResponse(false));

        mockMvc.perform(put("/api/notifications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Títol nou","message":"Missatge nou","type":"ERROR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(service).update(eq(ID), any(NotificationRequest.class));
    }

    @Test
    void updateReturns404WhenNotFound() throws Exception {
        when(service.update(eq(ID), any(NotificationRequest.class)))
                .thenThrow(new NotificationNotFoundException(ID));

        mockMvc.perform(put("/api/notifications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Títol","message":"Missatge"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns400WhenValidationFails() throws Exception {
        mockMvc.perform(put("/api/notifications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"          ","message":"Missatge"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- DELETE ----------

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/notifications/{id}", ID))
                .andExpect(status().isNoContent());

        verify(service).delete(ID);
    }

    @Test
    void deleteReturns404WhenNotFound() throws Exception {
        doThrow(new NotificationNotFoundException(ID)).when(service).delete(ID);

        mockMvc.perform(delete("/api/notifications/{id}", ID))
                .andExpect(status().isNotFound());
    }

    // ---------- PATCH ----------

    @Test
    void markAsReadReturns200WithUpdatedState() throws Exception {
        when(service.markAsRead(ID)).thenReturn(sampleResponse(true));

        mockMvc.perform(patch("/api/notifications/{id}/read", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAsReadReturns404WhenNotFound() throws Exception {
        when(service.markAsRead(ID)).thenThrow(new NotificationNotFoundException(ID));

        mockMvc.perform(patch("/api/notifications/{id}/read", ID))
                .andExpect(status().isNotFound());
    }
}
