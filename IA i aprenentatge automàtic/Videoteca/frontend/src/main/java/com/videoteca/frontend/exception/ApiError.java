package com.videoteca.frontend.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cos JSON d'error del microservici: { "error": "descripció" }.
 */
public record ApiError(@JsonProperty("error") String error) {
}
