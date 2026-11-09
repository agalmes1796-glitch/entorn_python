package com.videoteca.actors.web;

import com.videoteca.actors.domain.ActorNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Tradueix les excepcions del microservici a respostes HTTP amb el cos
 * d'error estàndard ({@link ErrorResponse}).
 *
 * <ul>
 *   <li>{@link ActorNotFoundException} → 404 (actor inexistents).</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 amb el detall dels
 *       camps no vàlids (validació del cos de la petició).</li>
 *   <li>Altres peticions malformades (JSON no llegible, recurs inexistents)
 *       → 400 o 404 segons el cas.</li>
 *   <li>Qualsevol altre error → 500 (es registra el rastre a la consola).</li>
 * </ul>
 */
@RestControllerAdvice
public class ActorExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ActorExceptionHandler.class);

    /** Actor inexistents → 404. */
    @ExceptionHandler(ActorNotFoundException.class)
    public ResponseEntity<ErrorResponse> actorNoEncontrat(ActorNotFoundException ex,
                                                          HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /**
     * Errors de validació del cos de la petició (Bean Validation al DTO) → 400
     * amb la llista de camps afectats.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> errorsDeValidacio(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<ErrorResponse.FieldError> camps = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return responder(request, HttpStatus.BAD_REQUEST,
                "La petició no és vàlida: revisa els camps indicats", camps);
    }

    /** Cos de la petició no llegible (JSON malformat, tipus incoherents) → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cosNoLlegible(HttpMessageNotReadableException ex,
                                                       HttpServletRequest request) {
        logger.debug("Cos de petició no llegible a {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(request, HttpStatus.BAD_REQUEST,
                "El cos de la petició no és un JSON vàlid o no coincideix amb l'estructura esperada", null);
    }

    /** Ruta inexistents (cap controlador la gestiona) → 404 en lloc de 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> recursInexistent(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND,
                "El recurs sol·licitat no existeix", null);
    }

    /**
     * Violació d'una restricció en persistir (cas reservat: la validació ja
     * passa pel DTO al controlador) → 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> violacioDeRestriccions(ConstraintViolationException ex,
                                                                HttpServletRequest request) {
        logger.debug("Violació de restriccions en persistir a {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return responder(request, HttpStatus.BAD_REQUEST,
                "Les dades enviades no compleixen les restriccions de la base de dades", null);
    }

    /** Qualsevol altre error no esperat → 500 (es registra el rastre complet). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorIntern(Exception ex, HttpServletRequest request) {
        logger.error("Error intern no esperat a {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(request, HttpStatus.INTERNAL_SERVER_ERROR,
                "Error intern del servidor: intenta-ho més tard o contacta amb el servei de suport", null);
    }

    /**
     * Construeix la resposta d'error estàndard amb el codi donat.
     */
    private ResponseEntity<ErrorResponse> responder(HttpServletRequest request,
                                                    HttpStatus status,
                                                    String message,
                                                    List<ErrorResponse.FieldError> camps) {
        ErrorResponse cos = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                camps);
        return ResponseEntity.status(status).body(cos);
    }
}
