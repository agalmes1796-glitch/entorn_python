package com.videoteca.movies.exception;

import com.videoteca.movies.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo global de errores de la API.
 *
 * <p>Formatea todas las respuestas de error con el mismo cuerpo JSON
 * coherente ({@link ErrorDetail}):
 * <pre>{@code
 * { "timestamp", "status", "error", "path", "message", "errors" (opcional) }
 * }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404: recurso no encontrado. */
    @ExceptionHandler(MovieNoEncontradaException.class)
    public ResponseEntity<ErrorDetail> handleMovieNoEncontrada(MovieNoEncontradaException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, request, ex.getMessage(), null);
    }

    /** 404: ruta no mapeada (sin handler ni recurso estático). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDetail> handleNoResource(NoResourceFoundException ex,
                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, request,
                "Recurso no encontrado: '/%s'".formatted(ex.getResourcePath()), null);
    }

    /**
     * 415: Content-Type no soportado (p. ej., text/plain donde la API espera
     * application/json).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                   HttpServletRequest request) {
        String contentType = ex.getContentType() == null ? "desconocido" : ex.getContentType().toString();
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, request,
                "Content type '%s' no está soportado (se espera application/json)".formatted(contentType),
                null);
    }

    /** 400: cuerpo de petición no válido (validación de {@code @RequestBody}). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        Map<String, List<String>> byField = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError ->
                        byField.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                                .add(fieldError.getDefaultMessage()));
        return buildResponse(HttpStatus.BAD_REQUEST, request,
                "El cuerpo de la petición no es válido", flatten(byField));
    }

    /** 400: parámetros (path/request) no válidos (ConstraintViolation). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest request) {
        Map<String, List<String>> byField = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation ->
                        byField.computeIfAbsent(violation.getPropertyPath().toString(), k -> new ArrayList<>())
                                .add(violation.getMessage()));
        return buildResponse(HttpStatus.BAD_REQUEST, request,
                "Los parámetros de la petición no son válidos", flatten(byField));
    }

    /**
     * 400: cuerpo de petición ilegible (JSON malformado, valor con tipo
     * incorrecto...). El detalle interno (p. ej., el mensaje de Jackson) solo
     * se loguea; al cliente se le devuelve un mensaje genérico.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetail> handleNotReadable(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        log.error("Cuerpo de la petición no legible en {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, request,
                "El cuerpo de la petición no es un JSON válido o contiene valores con formato incorrecto", null);
    }

    /**
     * 400: id de path con formato inválido (Spring lanza esta excepción al
     * convertir el {@code @PathVariable} a {@link Long}).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, request,
                "Valor no válido para el parámetro '%s': '%s'"
                        .formatted(ex.getName(), String.valueOf(ex.getValue())), null);
    }

    /** 500: cualquier otro error inesperado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado procesando {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, request,
                "Error interno del servidor", null);
    }

    /** Une varias violaciones del mismo campo en un solo mensaje. */
    private Map<String, String> flatten(Map<String, List<String>> byField) {
        Map<String, String> errors = new LinkedHashMap<>();
        byField.forEach((field, messages) -> errors.put(field, String.join("; ", messages)));
        return errors;
    }

    private ResponseEntity<ErrorDetail> buildResponse(HttpStatus status,
                                                      HttpServletRequest request,
                                                      String message,
                                                      Map<String, String> errors) {
        ErrorDetail body = new ErrorDetail(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                message,
                errors);
        return ResponseEntity.status(status).body(body);
    }
}
