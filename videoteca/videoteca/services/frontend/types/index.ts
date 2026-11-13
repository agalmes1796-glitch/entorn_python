/**
 * Model de dades d'una pel·lícula.
 *
 * Coincideix amb l'estructura que retorna el gateway a GET {gateway}/api/movies
 * i amb la taula `pelicula` de la base de dades de la Videoteca:
 *   id, titulo, anio_estreno, duracion_minutos, genero_id
 */
export interface Movie {
  id: number
  titulo: string
  anio_estreno: number | null
  duracion_minutos: number | null
  genero_id: number
}
