package com.videoteca.movies.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.videoteca.movies.controller.MovieController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Pruebas unitarias del {@link GlobalExceptionHandler}: fijan el estado HTTP
 * y el mensaje exacto de cada mapeo (M1/M2/M3/B4/B5), incluido el 409 de
 * integridad (que no se puede provocar en H2: no hay constraints).
 *
 * <p>Sin contexto de Spring: el handler es una clase sin dependencias.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** Parámetro {@code Long id} de {@code MovieController.findById}. */
    private static MethodParameter idParameter() throws NoSuchMethodException {
        Method method = MovieController.class.getMethod("findById", Long.class);
        return new MethodParameter(method, 0);
    }

    @Test
    void notFound_debeDevolver404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(new ResourceNotFoundException("Pelicula", 99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Pelicula con id 99 no encontrada");
        assertThat(response.getBody().fieldErrors()).isNotNull().isEmpty();
    }

    @Test
    void typeMismatch_debeDevolver400ConMensajeDelParametro() throws Exception {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", idParameter(), null);

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo("Valor inválido para el parámetro 'id': abc (se esperaba Long)");
    }

    @Test
    void metodoNoSoportado_debeDevolver405() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "PUT", "DELETE"));

        ResponseEntity<ApiError> response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().message())
                .isEqualTo("Método no soportado: PATCH (permitidos: GET, PUT, DELETE)");
    }

    @Test
    void mediaTypeNoSoportado_debeDevolver415() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ApiError> response = handler.handleMediaTypeNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().message()).isEqualTo("Content-Type no soportado: text/plain");
    }

    @Test
    void dataIntegrity_debeDevolver409ConMensajeEstable() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("detalle interno del driver"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message())
                .isEqualTo("Violación de una restricción de integridad (generoId/directorId inexistente u otra constraint). Revisa los datos.")
                // No filtra detalles internos del driver
                .doesNotContain("detalle interno del driver");
        assertThat(response.getBody().fieldErrors()).isNotNull().isEmpty();
    }

    @Test
    void validacionDeParametro_debeDevolver400ConErroresPorCampo() throws Exception {
        Method method = MovieController.class.getMethod("findById", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        ParameterValidationResult result = new ParameterValidationResult(
                parameter, " ", List.of(new MessageSourceResolvableStub("El título no puede ser vacío ni nulo")),
                null, null, null, (resolvable, type) -> resolvable);
        HandlerMethodValidationException ex = new HandlerMethodValidationException(
                MethodValidationResult.create(new MovieController(null), method, List.of(result)));

        ResponseEntity<ApiError> response = handler.handleParameterValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("La petición no es válida");
        assertThat(response.getBody().fieldErrors())
                .isNotNull()
                .containsEntry("id", "El título no puede ser vacío ni nulo");
    }

    @Test
    void jsonMalformado_debeDevolver400() {
        ResponseEntity<ApiError> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("mal json", (org.springframework.http.HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Cuerpo de la petición no legible (JSON malformado)");
    }

    @Test
    void parametroAusente_debeDevolver400() {
        ResponseEntity<ApiError> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("titulo", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Falta el parámetro requerido: titulo");
    }

    @Test
    void catchAll_debeDevolver500() {
        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
        // B5: fieldErrors nunca es null (map vacío)
        assertThat(response.getBody().fieldErrors()).isNotNull().isEmpty();
    }

    /** Stub de {@link org.springframework.context.MessageSourceResolvable} con un mensaje fijo. */
    private record MessageSourceResolvableStub(String message) implements org.springframework.context.MessageSourceResolvable {
        @Override
        public String[] getCodes() {
            return new String[0];
        }

        @Override
        public String getDefaultMessage() {
            return message;
        }
    }
}
