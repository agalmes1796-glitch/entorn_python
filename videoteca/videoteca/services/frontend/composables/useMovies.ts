import type { Movie } from '~/types'

/**
 * Carrega la llista de pel·lícules a través del propi endpoint d'aquest
 * microservi (/api/movies), que a la vegada crida al gateway.
 *
 * Amb aquest patró:
 *  - el navegador només coneix la URL relativa /api/movies (mateix origen);
 *  - la URL real del gateway mai s'exposa al client;
 *  - funciona tant amb SSR com amb client (useFetch la revalida al client).
 */
export function useMovies() {
  return useFetch<Movie[]>('/api/movies')
}
