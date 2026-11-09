package com.videoteca.notifications.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.time.Instant;

/**
 * Gestor global d'excepcions del microservici.
 *
 * <p>Converteix les excepcions del domini en respostes JSON amb l'estat
 * HTTP adequat:</p>
 * <ul>
 *   <li>{@link NotificationNotFoundException} → 404 (timestamp, status, error, message, path)</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 (incloent el camp i el missatge)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
}
