package com.videoteca.movies.exception;

/**
 * Se lanza cuando un recurso buscado por id no existe.
 *
 * <p>El {@link GlobalExceptionHandler} la traduce a una respuesta
 * HTTP 404 en JSON.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final Long id;

    public ResourceNotFoundException(String resource, Long id) {
        super("%s con id %s no encontrada".formatted(resource, id));
        this.resource = resource;
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public Long getId() {
        return id;
    }
}
