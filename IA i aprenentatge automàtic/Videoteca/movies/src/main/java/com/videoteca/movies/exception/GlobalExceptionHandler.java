package com.videoteca.movies.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce excepciones a respuestas JSON coherentes ({@link ApiError}).
 *
 * <ul>
 *   <li>404: recurso inexistente ({@link ResourceNotFoundException})</li>
 *   <li>400: validación de Bean del cuerpo ({@link MethodArgumentNotValidException}),
 *       restricciones sobre parámetros de la petición
 *       ({@link HandlerMethodValidationException}, p. ej. {@code @NotBlank} en un
 *       {@code @RequestParam}), JSON malformado
 *       ({@link HttpMessageNotReadableException}), parámetro de query ausente
 *       ({@link MissingServletRequestParameterException}) o tipo de parámetro/path
 *       variable incorrecto ({@link MethodArgumentTypeMismatchException}, que hereda de
 *       {@link org.springframework.core.convert.TypeMismatchException})</li>
 *   <li>405: método HTTP no soportado ({@link HttpRequestMethodNotSupportedException})</li>
 *   <li>415: Content-Type no soportado ({@link HttpMediaTypeNotSupportedException})</li>
 *   <li>409: restricción de integridad de la BD ({@link DataIntegrityViolationException},
 *       p. ej. generoId/directorId inexistente)</li>
 *   <li>500: cualquier otro error (red de seguridad; el mensaje no filtra detalles internos)</li>
 * </ul>
 *
 * <p>Los handlers específicos van ANTES del catch-all genérico: Spring resuelve
 * el más específico según el tipo de excepción, pero mantener este orden hace
 * explícita la prioridad de red de seguridad del catch-all {@code Exception}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    /**
     * En Spring Framework 7, {@code HttpMessageNotValidException} (fallos de
     * validación al deserializar el cuerpo) hereda de
     * {@code MethodArgumentNotValidException}, así que un único handler cubre
     * ambos casos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valor inválido",
                        (first, second) -> first));
        return build(HttpStatus.BAD_REQUEST, "La petición no es válida", fieldErrors);
    }

    /**
     * Restricciones Bean Validation sobre parámetros de método (p. ej.
     * {@code @NotBlank} en un {@code @RequestParam}): en Spring Framework 7 se
     * aplican vía {@code MethodValidator.applyArgumentValidation} y el fallo se
     * reporta como {@link HandlerMethodValidationException} (que hereda de
     * {@code ResponseStatusException}).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String field = result.getMethodParameter() != null
                    ? result.getMethodParameter().getParameterName()
                    : "parámetro";
            result.getResolvableErrors().stream()
                    .map(error -> error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Valor inválido")
                    .findFirst()
                    .ifPresent(message -> fieldErrors.putIfAbsent(field, message));
        }
        return build(HttpStatus.BAD_REQUEST, "La petición no es válida", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Cuerpo de la petición no legible (JSON malformado)", Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Falta el parámetro requerido: " + ex.getParameterName(), Map.of());
    }

    /**
     * Fallo al convertir un path variable o parámetro de query al tipo declarado
     * (p. ej. {@code GET /api/movies/abc} → {@code Long}): 400 en lugar de caer
     * en el catch-all 500. {@link MethodArgumentTypeMismatchException} hereda de
     * {@code TypeMismatchException}, que es el tipo que Spring lanza en el web
     * para estas conversiones.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "tipo desconocido";
        return build(HttpStatus.BAD_REQUEST,
                "Valor inválido para el parámetro '" + ex.getName() + "': " + ex.getValue()
                        + " (se esperaba " + requiredType + ")",
                Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        StringBuilder message = new StringBuilder("Método no soportado: ").append(ex.getMethod());
        if (ex.getSupportedMethods() != null && ex.getSupportedMethods().length > 0) {
            message.append(" (permitidos: ").append(String.join(", ", ex.getSupportedMethods())).append(')');
        }
        return build(HttpStatus.METHOD_NOT_ALLOWED, message.toString(), Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type no soportado: " + ex.getContentType(), Map.of());
    }

    /**
     * Restricción de integridad en la BD: generoId/directorId inexistente (FK de
     * {@code pelicula} hacia {@code genero}/{@code director}) u otra constraint.
     * Mensaje estable: no se exponen los detalles internos del driver.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "Violación de una restricción de integridad (generoId/directorId inexistente u otra constraint). Revisa los datos.",
                Map.of());
    }

    /**
     * Último recurso: cualquier excepción no mapeada → 500. Debe quedar al
     * final de la clase para que no eclipse a los handlers específicos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
