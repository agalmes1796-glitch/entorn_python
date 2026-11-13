package com.videoteca.notifications.api.web;

import com.videoteca.notifications.domain.exception.BadRequestException;
import com.videoteca.notifications.domain.exception.ConflictException;
import com.videoteca.notifications.domain.exception.NotificationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Manejo global de errores: respuestas JSON uniformes.
 *
 * <ul>
 *   <li>400: {@code {"error": "Bad Request", "errors": [{"field": ..., "message": ...}]}}</li>
 *   <li>404: {@code {"error": "Not Found", "message": ..., "path": ...}}</li>
 *   <li>409: {@code {"error": "Conflict", "message": ..., "path": ...}}</li>
 *   <li>500: genérico de seguridad, sin stacktrace en el JSON</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Errores de validación de Bean Validation sobre el body (@Valid). */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		List<ApiErrorDetail> details = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();
		return new ApiErrorResponse("Bad Request", details);
	}

	/**
	 * JSON no legible (body malformado): respuesta FIJA, sin reflejar el
	 * mensaje interno de Jackson. El detalle solo se loggea en debug.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		if (log.isDebugEnabled()) {
			log.debug("JSON no legible en {} {}", request.getMethod(), request.getRequestURI(), ex);
		}
		return new ApiErrorResponse("Bad Request",
				List.of(new ApiErrorDetail("body", "El cuerpo de la petición no es un JSON válido")));
	}

	/** Peticiones mal formadas del dominio (p. ej., PATCH /read sin valor). */
	@ExceptionHandler(BadRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleBadRequest(BadRequestException ex) {
		return new ApiErrorResponse("Bad Request", List.of(new ApiErrorDetail("request", ex.getMessage())));
	}

	/** Validación de parámetros (query/path) con restricciones (p. ej. limit=0). */
	@ExceptionHandler(HandlerMethodValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleParamValidation(HandlerMethodValidationException ex) {
		List<ApiErrorDetail> details = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new ApiErrorDetail(paramName(result), error.getDefaultMessage())))
			.toList();
		return new ApiErrorResponse("Bad Request", details);
	}

	/** Parámetro con un tipo no convertible (p. ej. ?status=INVALID). */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return new ApiErrorResponse("Bad Request",
				List.of(new ApiErrorDetail(ex.getName(), "Valor no válido para el parámetro '" + ex.getName() + "'")));
	}

	/** Conflicto de operación (p. ej., reenvío de una notificación ya enviada). */
	@ExceptionHandler(ConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ConflictErrorResponse handleConflict(ConflictException ex, HttpServletRequest request) {
		log.warn("Conflicto en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return new ConflictErrorResponse("Conflict", ex.getMessage(), request.getRequestURI());
	}

	/** Recurso de notificación inexistente o ruta no mapeada. */
	@ExceptionHandler({ NotificationNotFoundException.class, NoHandlerFoundException.class,
			NoResourceFoundException.class })
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public NotFoundErrorResponse handleNotFound(Exception ex, HttpServletRequest request) {
		String message;
		if (ex instanceof NoHandlerFoundException noHandler) {
			message = "Ruta no encontrada: " + noHandler.getHttpMethod() + " " + noHandler.getRequestURL();
		}
		else if (ex instanceof NoResourceFoundException noResource) {
			message = "Ruta no encontrada: " + noResource.getHttpMethod() + " " + noResource.getResourcePath();
		}
		else {
			message = ex.getMessage();
		}
		return new NotFoundErrorResponse("Not Found", message, request.getRequestURI());
	}

	/**
	 * Error inesperado: respuesta genérica de seguridad. El stacktrace solo va
	 * a los logs, nunca al JSON.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public InternalErrorResponse handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
		return new InternalErrorResponse("Internal Server Error", "Se produjo un error interno del servidor");
	}

	private String paramName(org.springframework.validation.method.ParameterValidationResult result) {
		if (result.getMethodParameter() != null && result.getMethodParameter().getParameterName() != null) {
			return result.getMethodParameter().getParameterName();
		}
		return "request";
	}

}
