package com.videoteca.actors.web;

import com.videoteca.actors.exception.ActorNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores de la API.
 *
 * <p>Las respuestas de error usan {@link ProblemDetail} (RFC 9457), nativo en
 * Spring Framework 7: {@code Content-Type: application/problem+json} con
 * {@code status}, {@code title}, {@code detail} y propiedades de detalle.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404: recurso no encontrado. */
    @ExceptionHandler(ActorNotFoundException.class)
    ProblemDetail handleActorNotFound(ActorNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Actor not found");
        return detail;
    }

    /** 400: cuerpo de petición no válido (validación de {@code @RequestBody}). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError ->
                        errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return badRequest("El cuerpo de la petición no es válido", errors);
    }

    /**
     * 400: cuerpo de petición ilegible (JSON malformado, {@code "birthDate": "abc"},
     * {@code "name": 123}...). El detalle expuesto al cliente es genérico: el
     * mensaje interno de Jackson ({@code ex.getMessage()}) solo se loguea,
     * con el stack completo.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("Cuerpo de la petición no legible en {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return badRequest(
                "El cuerpo de la petición no es un JSON válido o contiene valores con formato incorrecto",
                null);
    }

    /**
     * 415: Content-Type no soportado (p. ej., text/plain donde la API espera
     * application/json).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String contentType = ex.getContentType() == null ? "desconocido" : ex.getContentType().toString();
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type '%s' no está soportado (se espera application/json)".formatted(contentType));
    }

    /** 400: parámetros (path/request) no válidos (ConstraintViolation). */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation ->
                        errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return badRequest("Los parámetros de la petición no son válidos", errors);
    }

    /**
     * 400: id de path con formato inválido (Spring lanza esta excepción al
     * convertir el {@code @PathVariable} a {@link java.util.UUID}).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Valor no válido para el parámetro '%s': '%s'"
                .formatted(ex.getName(), String.valueOf(ex.getValue()));
        return badRequest(detail, null);
    }

    /**
     * 400: argumento inválido (p. ej., UUID malformado lanzado por la aplicación).
     * Se devuelve un mensaje genérico: el detalle interno
     * ({@code ex.getMessage()}) solo se loguea, no se expone al cliente.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage(), ex);
        return badRequest("Requisición no válida", null);
    }

    /**
     * 404: ruta no mapeada (sin handler ni recurso estático). En Spring 7 una
     * petición así lanza {@link NoResourceFoundException}; sin este handler la
     * capturaría el catch-all {@link Exception} y devolvería un 500 incorrecto.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Recurso no encontrado: '/%s'".formatted(ex.getResourcePath()));
        detail.setTitle("Not Found");
        return detail;
    }

    /** 500: cualquier otro error inesperado. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado procesando {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        detail.setTitle("Internal Server Error");
        return detail;
    }

    private ProblemDetail badRequest(String message, Map<String, String> errors) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Bad Request");
        if (errors != null && !errors.isEmpty()) {
            detail.setProperty("errors", errors);
        }
        return detail;
    }
}
