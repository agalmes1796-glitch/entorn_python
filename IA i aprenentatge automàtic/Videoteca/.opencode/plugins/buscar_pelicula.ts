
import { tool } from "@opencode-ai/plugin"
import mysql from "mysql2/promise"
import "dotenv/config"

export default tool({
  description: `
    Busca una película por su nombre en la base de datos
    MariaDB de Videoteca.

    Devuelve todos los datos de la película:
    id, título, sinopsis, año de estreno, duración,
    género y director.

    UTILIZA ESTA TOOL siempre que el usuario pida buscar
    una película por su nombre en la base de datos.
  `,

  args: {
    titulo: tool.schema.string().describe(
      "Título de la película que queremos encontrar"
    ),
  },

  async execute(args) {
    let connection: mysql.Connection | undefined

    try {
      connection = await mysql.createConnection({
        host: process.env.DB_HOST || "127.0.0.1",
        port: Number(process.env.DB_PORT || 3306),
        user: process.env.DB_USER || "videoteca",
        password: process.env.DB_PASSWORD || "videoteca_dev",
        database: process.env.DB_NAME || "videoteca",
        charset: "utf8mb4",
      })

      const [rows] = await connection.execute(
        `
          SELECT
            id,
            titulo,
            sinopsis,
            anio_estreno,
            duracion_minutos,
            genero_id,
            director_id
          FROM pelicula
          WHERE titulo LIKE ?
          ORDER BY
            CASE
              WHEN LOWER(titulo) = LOWER(?) THEN 0
              ELSE 1
            END
          LIMIT 10
        `,
        [`%${args.titulo.trim()}%`, args.titulo.trim()]
      )

      const peliculas = rows as mysql.RowDataPacket[]

      if (peliculas.length === 0) {
        return JSON.stringify({
          encontrado: false,
          mensaje: `No existe ninguna película llamada ${args.titulo}`,
        })
      }

      return JSON.stringify({
        encontrado: true,
        total: peliculas.length,
        peliculas,
      }, null, 2)

    } catch (error) {
      return JSON.stringify({
        encontrado: false,
        error: error instanceof Error
          ? error.message
          : String(error),
      })
    } finally {
      if (connection) {
        await connection.end()
      }
    }
  },
})
