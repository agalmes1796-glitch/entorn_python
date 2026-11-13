import { Plugin } from "@opencode/plugin"
import mariadb from 'mariadb';

export default Plugin.define({
    id: "movies",

    async setup(ctx) {
        await ctx.tool.transform((editor) => {
            editor.add({
                name: "search-by-name",
                description: "Search films by name. Name can be exactly or partial.",
                input: {
                    type: "object",
                    properties: {
                        name: {
                            type: "string",
                            description: "Name of the film. Name can be exactly or partial.",
                        }
                    },
                    required: ["name"],
                    additionalProperties: false,
                },

                async execute(input) {
                    const { name } = input as { name: string }

                    //Fer la connexió a MariaDB.
                    //Nota: la dependència 'mariadb' viu a node_modules del projecte; cal el pont
                    //de symlink .opencode/node_modules/mariadb perquè el runtime (Bun) pugui resoldre-la
                    //des d'aquesta carpeta amagada (.opencode).
                    const conn = await mariadb.createConnection({
                        host: 'localhost',
                        port: 3306,
                        user: 'videoteca',
                        password: 'videoteca_dev',
                        database: 'videoteca',
                    });

                    try {
                        const rows = await conn.query<{
                            id: number;
                            titulo: string;
                            anio_estreno: number | null;
                            duracion_minutos: number | null;
                            genero_id: number;
                        }[]>(
                            'SELECT id, titulo, anio_estreno, duracion_minutos, genero_id FROM pelicula WHERE titulo LIKE ? ORDER BY id',
                            [`%${name}%`],
                        );
                        return {
                            content: rows.length
                                ? JSON.stringify(rows, null, 2)
                                : `No s'ha trobat cap pel·lícula amb el nom '${name}'.`,
                        }
                    } finally {
                        await conn.end();
                    }
                },
            })
        })
    },
})