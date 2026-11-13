import type { Movie } from '~/types'

/**
 * Utilitat per cridar al *gateway* de la Videoteca.
 *
 * El gateway és l'únic punt d'accés a les dades des d'aquest microservi
 * (frontend -> gateway -> servei de pel·lícules / base de dades). Aquí
 * centralitzem com s'hi fa la crida perquè tots els endpoints d'aquest
 * servidor en facin ús de la mateixa manera.
 */

/** URL base del gateway (configuració de servidor, mai al client). */
export function getGatewayUrl(): string {
  const url = useRuntimeConfig().gatewayUrl
  // Coincideix amb el valor per defecto de nuxt.config.ts (gateway al host:8090).
  return typeof url === 'string' ? url : 'http://localhost:8090'
}

/**
 * Obté la llista completa de pel·lícules cridant al gateway:
 *   GET {gateway}/api/movies
 *
 * @returns Llista de pel·lícules (pot ser buida).
 * @throws Si el gateway no és accessible o respon amb un error.
 */
export async function fetchMoviesFromGateway(): Promise<Movie[]> {
  const movies = await $fetch<Movie[]>(`${getGatewayUrl()}/api/movies`)
  return Array.isArray(movies) ? movies : []
}
