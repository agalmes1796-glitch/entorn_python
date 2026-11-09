package com.videoteca.actors.dto;

import java.util.List;

/**
 * Cos de la resposta paginada de la llista d'actors
 * (GET {@code /api/actors?page=0&size=20}).
 *
 * @param content        actors de la pàgina sol·licitada
 * @param page           número de pàgina (començant per 0)
 * @param size           mida de pàgina sol·licitada
 * @param totalElements  total d'actors a la base de dades
 * @param totalPages     total de pàgines amb la mida sol·licitada
 */
public record ActorPageResponse(
        List<ActorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
