package com.videoteca.notifications.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Gestor global d'excepcions del microservici.
 *
 * <p>Converteix les excepcions del domini en respostes JSON amb l'estat
 * HTTP adequat:</p>
 * <ul>
 *   <li>{@link NotificationNotFoundException} → 404 (timestamp, status, error, message, path)</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 (incloent el camp i el missatge)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} → 400 (paràmetre amb un valor que no es pot convertir,
 *       p. ex., un id que no és un UUID o {@code ?read=xyz})</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (cos de la petició amb un JSON invàlid o amb
 *       valors que no encaixen amb el tipus dels camps, p. ex., un {@code type} que no és un enum)</li>
 *   <li>Qualsevol altra excepció → 500 amb missatge genèric (el stack trace es registra al servidor)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Notificació inexistente → 404 Not Found. */
    @ExceptionHandler(NotificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotificationNotFoundException ex, HttpServletRequest request) {
        return new ApiError(Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    /** Petició amb camps que no validen → 400 Bad Request amb el camp afectat. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String field = (fieldError != null) ? fieldError.getField() : null;
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : ex.getMessage();
        return new ApiError(Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                field);
    }

    /** Paràmetre amb un valor que no es pot convertir al seu tipus → 400 amb el paràmetre afectat. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return new ApiError(Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Valor invàlid per al paràmetre '" + ex.getName() + "'",
                request.getRequestURI(),
                ex.getName());
    }

    /** Cos de la petició no llegible com a JSON (JSON malformat o valor incompatible amb un camp) → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return new ApiError(Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cos de la petició no és un JSON vàlid",
                request.getRequestURI(),
                null);
    }

    /** Excepció inesperada → 500 amb missatge genèric; el stack trace es registra al servidor. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperat processant la petició a " + request.getRequestURI(), ex);
        return new ApiError(Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Internal error",
                request.getRequestURI(),
                null);
    }
}
