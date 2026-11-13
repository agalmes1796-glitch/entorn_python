import { fetchMoviesFromGateway } from '../utils/gateway'

/**
 * GET /api/movies
 *
 * Endpoint públic d'aquest microservi: retorna el llistat de pel·lícules.
 * Internament *crida al gateway* ({NUXT_GATEWAY_URL}/api/movies), de manera
 * que la URL del gateway mai arriba al navegador.
 */
export default defineEventHandler(async () => {
  try {
    return await fetchMoviesFromGateway()
  }
  catch (error) {
    const detail = error instanceof Error ? error.message : String(error)
    throw createError({
      statusCode: 502,
      statusMessage: 'Gateway no disponible',
      message: `No s'ha pogut contactar amb el gateway per llistar les pel·lícules: ${detail}`,
    })
  }
})
