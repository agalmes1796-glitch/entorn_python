# Videoteca — `frontend` (microservi)

Microservi de frontend de la **Videoteca** construït amb [Nuxt 3](https://nuxt.com).
Mostra el catàleg complet de pel·lícules.

## Arquitectura

Aquest microservi **no llegeix la base de dades directament**. Totes les crides
a les dades es fan a través del *gateway* (Spring Cloud Gateway):

```
Navegador ──► frontend (Nuxt) ──► gateway ──► servei de pel·lícules ──► DB
             /api/movies          /api/movies    (lb:// via Eureka)
```

- El frontend exposa un únic endpoint: **`GET /api/movies`**
  (`server/api/movies.get.ts`).
- Aquest endpoint crida al gateway: **`GET {NUXT_GATEWAY_URL}/api/movies`**.
- La URL del gateway és una configuració de servidor (`runtimeConfig.gatewayUrl`);
  **mai s'exposa al navegador**. El client només coneix la ruta relativa
  `/api/movies` (mateix origen).

> El *gateway* (`services/gateway`) ja existeix i es registra a
> **Eureka** (`services/eureka`). Actualment la ruta de pel·lícules
> al gateway encara no és activa (a `application.yml` hi ha un exemple
> comentat). Caldrà afegir-hi una ruta `Path=/api/movies/**` que apunti al
> servei de pel·lícules per tal que el flux complet funcioni. Aquest repositori
> aquí només conté el `frontend`.

## Contracte esperat del gateway

El `frontend` espera que el gateway exposi:

```
GET {NUXT_GATEWAY_URL}/api/movies
→ 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "titulo": "Títol de la pel·lícula",
    "anio_estreno": 1999,
    "duracion_minutos": 119,
    "genero_id": 3
  }
]
```

Els camps coincideixen amb la taula `pelicula` de la base de dades
(`id`, `titulo`, `anio_estreno`, `duracion_minutos`, `genero_id`).
Si el gateway no és accessible (o no té la ruta de pel·lícules), el frontend
retorna un `502` i la interfície mostra l'estat d'error amb un botó per
tornar-ho a provar.

## Configuració (variables d'entorn)

| Variable           | Per defecto            | Descripció                                             |
| ------------------ | ---------------------- | ------------------------------------------------------ |
| `NUXT_GATEWAY_URL` | `http://localhost:8090`| URL base del gateway (només visible al servidor).      |

Copia `.env.example` a `.env` per personalitzar-lo:

```bash
cp .env.example .env
```

## Ports

| Servici   | Port (host)     | Notes                                                |
| --------- | --------------- | ---------------------------------------------------- |
| frontend  | `127.0.0.1:3001`| Dev (`npm run dev`) i Docker (port intern 3001).     |
| gateway   | `127.0.0.1:8090`| Gateway (port intern del contenedor: 8080).          |
| eureka    | `127.0.0.1:8761`| Registrador de serveis.                              |

> En Docker, el frontend apunta al gateway amb `http://gateway:8080`
> (nom del servei + port intern), no amb `localhost`.

## Ús local (desenvolupament)

Prerequisits: Node.js ≥ 20 i npm.

```bash
cd services/frontend
npm install
cp .env.example .env      # opcional; ja hi ha un valor per defecto
npm run dev               # http://localhost:3001
```

Llança el gateway (i Eureka) prèviament des de l'arrel del projecte:

```bash
docker compose up --build eureka gateway
```

Altres scripts:

```bash
npm run build              # build de producció (.output)
npm run preview            # previsualitza el build (http://localhost:3001)
npm run typecheck          # comprovació de tipus
```

## Ús amb Docker

```bash
# Des de l'arrel del projecte
docker compose up --build frontend
# → http://localhost:3001
```

El servei `frontend` es troba a `docker-compose.yaml` (arrel del projecte) i
`depends_on` el `gateway` perquè s'iniciï després que el gateway sigui sa.

## Estructura

```
services/frontend/
├── app.vue                  # Componente raíz (munta NuxtPage)
├── assets/css/main.css      # Estils globals
├── components/
│   ├── MovieList.vue        # Llista + estats (càrrega/error/buit)
│   └── MovieCard.vue        # Targeta individual de pel·lícula
├── composables/useMovies.ts # useFetch('/api/movies')
├── pages/index.vue          # Pàgina / (catàleg)
├── public/favicon.svg
├── server/
│   ├── api/movies.get.ts    # GET /api/movies  →  crida al gateway
│   └── utils/gateway.ts     # Helper central per cridar al gateway
├── types/index.ts           # Interfície Movie
├── nuxt.config.ts
├── package.json
├── tsconfig.json
├── .env.example
├── .gitignore
└── Dockerfile
```
