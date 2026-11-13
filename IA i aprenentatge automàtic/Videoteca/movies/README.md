# Microservicio Movies (Videoteca)

CRUD REST de películas sobre la tabla `pelicula` existente de la base de datos
del proyecto **Videoteca**.

## Stack

| Tecnología | Versión |
|---|---|
| Java | **25** |
| Spring Boot | **4.1.2** (parent `spring-boot-starter-parent`, común a todo el proyecto) |
| Spring Framework | **7.0.10** (MVC + PathPatterns) |
| Jackson | **3** (`tools.jackson`, gestionado por Boot 4) |
| Spring Cloud | **2025.1.4** (BOM `spring-cloud-dependencies`, cliente Eureka) |
| Build | Maven 3.9+ (`com.videoteca:movies:1.0.0`, jar `target/app.jar`) |
| JPA / ORM | Spring Data JPA + Hibernate 7 (`ddl-auto: validate`) |
| DB | MariaDB (driver `org.mariadb.jdbc.Driver`, dialect `MariaDBDialect`) |
| Registro de servicios | Netflix Eureka (servicio id `movies`) |
| Puerto | **8084** (8083 lo reserva el microservicio `frontend` en docker-compose) |

> Nota: se mantiene `spring-boot-starter-web` (y no
> `spring-boot-starter-webmvc`) por coherencia con el resto de microservicios
> del proyecto; la migración es backlog transversal (ver "Limitaciones /
> backlog"). Los tests web usan `spring-boot-starter-webmvc-test`
> (`@AutoConfigureMockMvc` vive en el módulo `spring-boot-webmvc-test` en
> Boot 4, verificado en 4.1.2).

## Prerrequisitos

La base de datos debe estar corriendo. Desde la raíz del proyecto Videoteca:

```bash
docker compose up -d mariadb
# schema: videoteca | usuario: videoteca | password: videoteca_dev
```

La tabla `pelicula` debe existir (ya existe en el proyecto; Hibernate **no**
la crea: `ddl-auto: validate` solo comprueba que el esquema coincida).

## Arranque

```bash
# Opción A: modo desarrollo
mvn spring-boot:run

# Opción B: empaquetar y ejecutar
mvn -q -DskipTests package
java -jar target/app.jar
```

Verificación rápida (puerto **8084**):

```bash
curl http://127.0.0.1:8084/actuator/health     # → {"status":"UP",...}
curl http://127.0.0.1:8084/                    # → listado JSON (snake_case)
curl http://127.0.0.1:8084/api/movies          # → listado JSON (idéntico)
```

## Contrato con el gateway / frontend

**Este microservicio se consume a través del gateway (Spring Cloud Gateway
5.x), que enruta `/<servei>/**` → `lb://<servei>/**` con `StripPrefix=1`
(discovery locator, `lower-case-service-id: true`).** Consecuencias, ya
implantadas y verificadas:

1. **Ruta raiz**: cuando el `frontend` llama a
   `GET {gateway}/movies`, el gateway reenvía la petición al servicio
   `movies` como `GET /` (el prefijo `/movies` se quita). Por eso
   `MovieController` expone todos los endpoints bajo **dos bases
   equivalentes**: `"/"` (contrato gateway/frontend) y `"/api/movies"`
   (contrato de uso directo). Los dos funcionan de verdad; la suite de
   integración cubre el ciclo completo sobre la raiz
   (`POST /` → 201 + `Location: http://host/{id}`, `GET /`, `GET /{id}`,
   `DELETE /{id}`) y el CRUD completo sobre `/api/movies`.

   > Nota técnica (Spring 7.0.10, verificado): la base raiz debe declararse
   > como `@RequestMapping({"/", "/api/movies"})` y **no** `{"", ...}`:
   > `PathPatternParser.parse("").matches(PathContainer.parsePath("/"))`
   > devuelve `false`, mientras que `parse("/").matches(...)` devuelve
   > `true`. Con `""` el `POST /` cae en `NoResourceFoundException` → 500.
   >
   > Efecto colateral documentado: cualquier petición mono-segmento sobre
   > raiz (`GET /api` sin `/movies`, `GET /favicon.ico`) encaja en
   > `GET /{id}` y devuelve 400 (id no numérico) en vez de 404; las rutas de
   > 2+ segmentos (`/actuator/health`, `/api/movies`) no se ven afectadas.

2. **JSON en snake_case global (peticiones Y respuestas)**: el DTO de
   deserialización del frontend (`Pelicula`) declara `@JsonProperty` con los
   nombres de columna de la tabla `pelicula`: `id`, `titulo`, `sinopsis`,
   `anio_estreno`, `duracion_minutos`, `genero_id`, `director_id`. Este
   microservicio responde (y acepta) exactamente esas claves, fijando la
   naming strategy de forma global en `application.yml`:

   ```yaml
   spring:
     jackson:
       property-naming-strategy: SNAKE_CASE
   ```

   En Boot 4.1.2 + Jackson 3 (`tools.jackson`) la propiedad se aplica al
   autoconfigure sin customizer extra: `JacksonAutoConfiguration`
   (`spring-boot-jackson` 4.1.2) → `AbstractMapperBuilderCustomizer
   .configurePropertyNamingStrategy` → `PropertyNamingStrategies.SNAKE_CASE`
   sobre el bean `JsonMapper`, que a su vez alimenta el
   `JacksonJsonHttpMessageConverter` de MVC (verificado en los binarios de
   4.1.2, no es necesario un `JacksonCustomizer`/`@Bean JsonMapper`).
   **Verificado con curl real** contra la instancia arrancada:
   `GET /` y `GET /api/movies` devuelven las claves
   `anio_estreno`, `duracion_minutos`, `genero_id`, `director_id`, y un
   `POST` con cuerpo snake_case deserializa y crea (201 + `Location`).

   - La naming strategy renombra **solo los campos de los DTO**
     (records/POJOs); **nunca las claves de los `Map`**. Por eso las claves
     del mapa `fieldErrors` de `ApiError` siguen siendo nombres de propiedad
     Java (camelCase), p. ej.
     `{"field_errors": {"generoId": "..."}}`.
   - Los cuerpos de petición en camelCase **ya no se aceptan** (los campos
     quedan `null` → 400 de validación). El wire format es snake_case, punto.

3. **Cabecera `Location` coherente con la base usada**: `POST /` →
   `Location: http://host/{id}`; `POST /api/movies` →
   `Location: http://host/api/movies/{id}` (se construye con
   `ServletUriComponentsBuilder.fromCurrentRequest()`).

## Eureka

El microservicio se registra en el servidor Eureka con el id **`movies`**
(`spring.application.name: movies`), que es el nombre por el que el gateway
resuelve el servicio (discovery locator).

- URL por defecto: `http://localhost:8761/eureka`
  (`application.yml` → `eureka.client.service-url.defaultZone`).
- Sobrescribible en runtime con la variable
  `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (docker-compose la fija a
  `http://eureka:8761/eureka` dentro de la red de Docker).
- **Si Eureka no está disponible la app arranca igualmente**: el cliente solo
  emite warnings de fallo de registro y reintenta periódicamente (comportamiento
  por defecto; no hay ningún default que bloquee el arranque local sin Eureka).
- Para desactivar el cliente por completo: `EUREKA_CLIENT_ENABLED=false`
  (`eureka.client.enabled: false`). Los tests ya lo llevan desactivado en
  `src/test/resources/application.yml`.

## Endpoints

| Método | Ruta (raiz, contrato gateway) | Ruta (uso directo) | Descripción | Respuesta |
|---|---|---|---|---|
| GET | `/` | `/api/movies` | Listado completo | `200` + `MovieResponse[]` (snake_case) |
| GET | `/{id}` | `/api/movies/{id}` | Película por id | `200` / `400` (id no numérico) / `404` |
| POST | `/` | `/api/movies` | Crear película | `201` + `Location` / `400` / `409` |
| PUT | `/{id}` | `/api/movies/{id}` | Actualizar película | `200` / `400` / `404` / `409` |
| DELETE | `/{id}` | `/api/movies/{id}` | Eliminar película | `204` / `400` (id no numérico) / `404` |
| GET | `/search?titulo=` | `/api/movies/search?titulo=` | Búsqueda por título (LIKE literal, case-insensitive) | `200` + `MovieResponse[]` / `400` (término vacío o ausente) |
| GET | — | `/actuator/health` | Health (Actuator) | `200` |

Cualquier método/Content-Type no soportado en estas rutas devuelve `405` /
`415` (p. ej. `PATCH /1` → 405; `POST /api/movies` con
`Content-Type: text/plain` → 415).

### Ejemplos curl (JSON snake_case)

```bash
# Listar (los dos equivalentes)
curl http://127.0.0.1:8084/
curl http://127.0.0.1:8084/api/movies
# → [
#     {"id":1,"titulo":"ACADEMY DINOSAUR","sinopsis":"A Epic Drama of a
#      Feminist And a Mad Scientist who must Battle a Teacher in The Canadian
#      Rockies","anio_estreno":2006,"duracion_minutos":86,"genero_id":6,
#      "director_id":null},
#     ...
#   ]

# Obtener por id
curl http://127.0.0.1:8084/1
curl http://127.0.0.1:8084/api/movies/1
# → {"id":1,"titulo":"ACADEMY DINOSAUR","sinopsis":"...","anio_estreno":2006,
#    "duracion_minutos":86,"genero_id":6,"director_id":null}

# Crear (201 + Location: http://<host>/{id} si se usa la base raiz,
# http://<host>/api/movies/{id} si se usa /api/movies)
curl -i -X POST http://127.0.0.1:8084/ \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Interestelar",
    "sinopsis": "Un viaje más allá del sistema solar.",
    "anio_estreno": 2014,
    "duracion_minutos": 148,
    "genero_id": 1,
    "director_id": null
  }'
# → HTTP/1.1 201
#   Location: http://127.0.0.1:8084/1001
#   {"id":1001,"titulo":"Interestelar","sinopsis":"Un viaje más allá del
#    sistema solar.","anio_estreno":2014,"duracion_minutos":148,
#    "genero_id":1,"director_id":null}

curl -i -X POST http://127.0.0.1:8084/api/movies \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Interestelar","anio_estreno":2014,"duracion_minutos":148,
       "genero_id":1,"director_id":null}'
# → Location: http://127.0.0.1:8084/api/movies/1002

# Actualizar
curl -X PUT http://127.0.0.1:8084/api/movies/1001 \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Interestelar (remasterizado)","sinopsis":"...",
       "anio_estreno":2014,"duracion_minutos":150,"genero_id":1}'

# Borrar (204)
curl -i -X DELETE http://127.0.0.1:8084/api/movies/1001
curl -i -X DELETE http://127.0.0.1:8084/1001

# Buscar por título (los comodines % y _ se tratan como literales)
curl "http://127.0.0.1:8084/search?titulo=inter"
curl "http://127.0.0.1:8084/api/movies/search?titulo=inter"
```

### Formato de error (4xx/5xx)

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Pelicula con id 999 no encontrada",
  "timestamp": "2026-11-06T10:00:00.123Z",
  "field_errors": {}
}
```

`timestamp` es un `Instant` UTC en ISO-8601 con milisegundos (o microsegundos,
según la precisión del reloj). `field_errors` **nunca es null**: es un objeto
vacío `{}` cuando no hay errores por campo.

> Nomenclatura: el componente del record `ApiError` `fieldErrors` se
> serializa como `field_errors` (naming strategy global SNAKE_CASE). Las
> **claves del mapa** `field_errors`, en cambio, **no** se renuevan (una
> naming strategy no afecta a las claves de un `Map`): son los nombres de
> propiedad Java en camelCase (`titulo`, `generoId`, `anioEstreno`,
> `duracionMinutos`), coherentes con `FieldError::getField` de Bean
> Validation.

Ejemplos de cada mapeo:

- **400** (validación de cuerpo) — `field_errors` con el detalle por campo:

  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "La petición no es válida",
    "field_errors": {
      "titulo": "El título no puede ser vacío ni nulo",
      "generoId": "El género (generoId) es obligatorio",
      "anioEstreno": "El año de estreno debe estar entre 1000 y 9999"
    }
  }
  ```

- **400** (parámetro de tipo incorrecto, p. ej. `GET /api/movies/abc`):

  ```json
  { "status": 400, "error": "Bad Request",
    "message": "Valor inválido para el parámetro 'id': abc (se esperaba Long)",
    "field_errors": {} }
  ```

- **405** (método no soportado, p. ej. `PATCH /api/movies/1`):

  ```json
  { "status": 405, "error": "Method Not Allowed",
    "message": "Método no soportado: PATCH (permitidos: GET, PUT, DELETE)",
    "field_errors": {} }
  ```

- **415** (Content-Type no soportado, p. ej. `POST /api/movies` con `text/plain`):

  ```json
  { "status": 415, "error": "Unsupported Media Type",
    "message": "Content-Type no soportado: text/plain",
    "field_errors": {} }
  ```

- **409** (restricción de integridad de la BD, p. ej. `genero_id` inexistente):

  ```json
  { "status": 409, "error": "Conflict",
    "message": "Violación de una restricción de integridad (generoId/directorId inexistente u otra constraint). Revisa los datos.",
    "field_errors": {} }
  ```

- **500** (error interno no mapeado; no filtra detalles):

  ```json
  { "status": 500, "error": "Internal Server Error",
    "message": "Error interno del servidor", "field_errors": {} }
  ```

### Validación de entradas

Los cuerpos se validan según estas reglas (las claves del JSON van en
snake_case, ver arriba):

- `titulo`: **obligatorio** (`@NotBlank`), máximo 128 caracteres
  (`@Size`) — la columna real es `varchar(128)`.
- `sinopsis`: opcional, máximo 65535 caracteres (`@Size`) — columna `TEXT`.
- `anio_estreno`: opcional, `1000`–`9999` (`@Min`/`@Max`) — columna `year(4)`.
- `duracion_minutos`: opcional, `0`–`65535` (`@Min`/`@Max`) —
  `smallint unsigned`.
- `genero_id`: **obligatorio** (`@NotNull`) — la columna `genero_id` es
  `NOT NULL`. Si no existe en la tabla `genero`, la BD devuelve **409**.
- `director_id`: opcional. Si no existe en la tabla `director`, la BD devuelve
  **409**.
- `?titulo=` en la búsqueda: **no vacío** (`@NotBlank` sobre el
  `@RequestParam` → 400); un término vacío daría `LIKE '%%'` y devolvería todo
  el listado.

## Variables de entorno

| Variable | Por defecto |
|---|---|
| `MOVIES_SERVER_PORT` | `8084` |
| `MOVIES_DB_URL` | `jdbc:mariadb://127.0.0.1:3306/videoteca` |
| `MOVIES_DB_USER` | `videoteca` |
| `MOVIES_DB_PASSWORD` | `videoteca_dev` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` |
| `EUREKA_INSTANCE_HOSTNAME` | (hostname de la máquina) |
| `EUREKA_CLIENT_ENABLED` | `true` |

> Nota: usa el prefijo `jdbc:mariadb://` (no `jdbc:mysql://`): el driver
> `org.mariadb.jdbc.Driver` (Connector/J ≥ 3.0) solo acepta
> `jdbc:mariadb://`; `jdbc:mysql://` requiere añadir `?permitMysqlScheme` a la
> URL.

Ejemplo:

```bash
MOVIES_DB_URL=jdbc:mariadb://10.0.0.5:3306/videoteca \
MOVIES_DB_USER=prod MOVIES_DB_PASSWORD=... \
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka.interno:8761/eureka \
  java -jar target/app.jar
```

## Tests

```bash
mvn test
```

- `MoviesApplicationTests` — carga del contexto completo (`@SpringBootTest`).
- `MovieServiceImplTest` — unitario del service con Mockito (sin DB, sin Spring).
- `MovieControllerIntegrationTest` — CRUD completo vía MockMvc (estados HTTP
  201+Location exacta, 200, 204, 404, 400, 405, 415, validación de rangos,
  búsqueda con término vacío y con comodines literales). Cubre **ambas bases**:
  el ciclo completo sobre la ruta raiz (`POST /` → 201 + `Location` exacta
  `http://localhost/{id}`, `GET /`, `GET /{id}`, `DELETE /{id}`, `GET
  /search`) y el CRUD completo sobre `/api/movies`. Los cuerpos de petición
  y los asertos de respuesta van en snake_case (el test context repite la
  `spring.jackson.property-naming-strategy: SNAKE_CASE` de producción: el
  test `application.yml` sustituye al principal, no lo mergea).
- `MovieControllerErrorMappingTest` — MockMvc (`standaloneSetup`) con el
  repository simulado lanzando `DataIntegrityViolationException` → 409. Como
  el setup standalone no carga el autoconfigure de Jackson de Boot, registra
  explícitamente el `JacksonJsonHttpMessageConverter` (Spring 7, Jackson 3)
  con un `JsonMapper` en `SNAKE_CASE`, replicando el contrato de producción.
- `GlobalExceptionHandlerTest` — unitario del handler: estado y mensaje exacto
  de cada mapeo (404/400/405/415/409/500) y `fieldErrors` nunca null.

Los tests usan **H2 en memoria en modo MariaDB** (`src/test/resources/application.yml`)
con `eureka.client.enabled: false` (no hay Eureka en los tests), de modo que
`mvn test` funciona en cualquier máquina sin arrancar la base de datos ni
Eureka. La dependencia H2 está en `scope=test` y **no** forma parte del
runtime.

## Docker

Imagen multi-stage (build con `maven:3.10.0-eclipse-temurin-25` → runtime
`eclipse-temurin:25-jre-noble` + `curl` para el healthcheck, usuario no root):

```bash
docker build -t videoteca-movies:1.0.0 .
docker run --rm -p 127.0.0.1:8084:8084 videoteca-movies:1.0.0
```

### docker-compose

El servicio **ya está incluido** en el `docker-compose.yaml` de la raíz del
proyecto (no hace falta pegar nada): se llama `movies` y expone
`127.0.0.1:8084:8080`. La entrada actual:

```yaml
  # Microservici Movies: CRUD REST de pel·lícules (servei "movies" a Eureka per al gateway)
  movies:
    build:
      context: ./movies
    image: videoteca-movies:1.0.0
    container_name: videoteca_movies
    restart: unless-stopped
    ports:
      # 8084 a l'host: el 8083 ja l'ocupa el frontend.
      - "127.0.0.1:8084:8080"
    environment:
      # Port intern del container: 8080, com el resto de microservicis (el port
      # per defecte de l'aplicació, 8084, és el de desenvolupament local fora de Docker).
      MOVIES_SERVER_PORT: 8080
      # Apunta al servidor Eureka dins de la xarxa de Docker (sobreescriu el valor per defecte del application.yml).
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka:8761/eureka
      # Es registra amb el nom del servei perquè sigui resolt pel DNS intern de Docker.
      EUREKA_INSTANCE_HOSTNAME: movies
      # BD dins de la xarxa de Docker (sobreescriu el valor per defecte del application.yml).
      MOVIES_DB_URL: jdbc:mariadb://mariadb:3306/videoteca
      MOVIES_DB_USER: ${MARIADB_USER:-videoteca}
      MOVIES_DB_PASSWORD: ${MARIADB_PASSWORD:-videoteca_dev}
    depends_on:
      eureka:
        condition: service_healthy
      mariadb:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health | grep -q '\"UP\"'"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 60s
```

Arrancar todo: `docker compose up -d --build` (o solo
`docker compose up -d --build movies`).

## Estructura

```
movies/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── README.md
└── src
    ├── main
    │   ├── java/com/videoteca/movies
    │   │   ├── MoviesApplication.java
    │   │   ├── controller/MovieController.java
    │   │   ├── dto/MovieRequest.java, MovieResponse.java
    │   │   ├── entity/Movie.java
    │   │   ├── exception/ApiError.java, GlobalExceptionHandler.java,
    │   │   │   ResourceNotFoundException.java
    │   │   ├── mapper/MovieMapper.java
    │   │   ├── repository/MovieRepository.java
    │   │   └── service/MovieService.java, service/impl/MovieServiceImpl.java
    │   └── resources/application.yml
    └── test
        ├── java/com/videoteca/movies/…
        └── resources/application.yml   (H2 en memoria + Eureka desactivado, solo tests)
```

## Limitaciones / backlog

- **Listado sin paginación y búsqueda `LIKE '%…%'`**: full scan en
  `pelicula.titulo`. Con ~1000 filas es aceptable; si el catálogo crece,
  añadir `Pageable` al listado y evaluar índice FULLTEXT / búsqueda dedicada.
- **Sin Spring Security**: entorno de desarrollo; antes de producción,
  autenticar en el microservicio o confiar en gateway + red (no exponer el
  puerto directamente).
- **Techo de la BD**: `id smallint unsigned` → máximo **65.535** películas
  (y `duracionMinutos`/`generoId`/`directorId` también smallint).
- **`spring-boot-starter-web` → `spring-boot-starter-webmvc`**: en Boot 4 el
  starter clásico queda marcado deprecated; se mantiene por coherencia de
  proyecto. Migración de una línea cuando el proyecto lo decida (backlog
  transversal a los 5 microservicios).
- **Credenciales dev en claro** (`videoteca` / `videoteca_dev` como
  defaults): en producción, secrets manager / vault, sin defaults en el yml.

## Decisiones y notas

- **Contrato gateway/frontend (ruta raiz + snake_case)**: el discovery
  locator del gateway hace `StripPrefix=1` sobre `/<servei>/**`, así que
  `GET {gateway}/movies` llega a este servicio como `GET /`. El controller
  usa `@RequestMapping({"/", "/api/movies"})` (una sola clase; el mapping de
  clase con varias rutas se compone con las sub-rutas de cada método en
  Spring 7, verificado). **La base raiz es `"/"` y no `""`**: probado con
  Spring 7.0.10, el patrón vacío `""` no matchea la petición `/`
  (`PathPatternParser.parse("").matches(PathContainer.parsePath("/")) ==
  false`) y el `POST /` caería en `NoResourceFoundException` → 500.
- **JSON snake_case global vía `spring.jackson.property-naming-strategy:
  SNAKE_CASE`**: probada la propiedad primero (según el plan de trabajo) y se
  aplica en Boot 4.1.2 + Jackson 3 sin customizer: `JacksonAutoConfiguration`
  (`spring-boot-jackson` 4.1.2) la resuelve vía
  `AbstractMapperBuilderCustomizer#configurePropertyNamingStrategy`
  (`ReflectionUtils.findField(PropertyNamingStrategies, "SNAKE_CASE")`) y la
  aplica al bean `JsonMapper`, que alimenta el
  `JacksonJsonHttpMessageConverter` de MVC (`JacksonHttpMessageConvertersConfiguration`
  de `spring-boot-http-converter` 4.1.2). Verificado con curl real:
  respuestas con `anio_estreno`, `duracion_minutos`, `genero_id`,
  `director_id`; POST con cuerpo snake_case → 201. La propiedad también está
  repetida en `src/test/resources/application.yml` porque el test yml
  **sustituye** al principal (no mergea).
- **`field_errors` vs. claves de mapa camelCase**: la naming strategy renueva
  el componente del record `ApiError` (`fieldErrors` → `field_errors`) pero
  no las claves del `Map` (siguen siendo `titulo`, `generoId`, `anioEstreno`,
  `duracionMinutos`, nombres de propiedad Java). Documentado también en el
  javadoc de `ApiError`.
- **Puerto 8084**: en docker-compose 8080 (phpMyAdmin), 8081 (gateway), 8082
  (kafka) y **8083 (frontend)** ya están ocupados en el host; el microservicio
  usa 8084 (primero libre). Dentro del contenedor de compose escucha en 8080
  (`MOVIES_SERVER_PORT=8080`, como el resto de servicios) y el host expone el
  8084: `127.0.0.1:8084:8080`.
- **URL `jdbc:mariadb://`**: el driver `org.mariadb.jdbc.Driver`
  (Connector/J ≥ 3.0) solo acepta `jdbc:mariadb://`; `jdbc:mysql://` requiere
  `?permitMysqlScheme`.
- **`ddl-auto: validate`**: la tabla `pelicula` es un activo del proyecto
  (tiene 1000+ películas). Hibernate solo valida el esquema; si falta la
  tabla o la BD no está disponible, el arranque falla de forma intencional
  (fail-fast). En entornos sin DB, ejecuta `mvn test` (H2 en memoria).
- **Tipos Java y validación del esquema**: la tabla usa columnas `smallint`
  (id, duracion_minutos, genero_id, director_id) y `year(4)` (anio_estreno).
  Hibernate 7 valida tipos de forma estricta y la tolerancia entre enteros es
  unilateral (la entidad puede ser más estrecha, nunca más amplia), así que
  `Long`/`Integer` sin más fallarían. Se resuelve en la entidad (documentado
  en el javadoc de `Movie`):
  - `@JdbcTypeCode(SqlTypes.SMALLINT)` sobre las columnas smallint
    (manteniendo `Long`/`Integer`; el binding runtime convierte bien).
  - `columnDefinition = "year(4)"` sobre `anioEstreno` (la validación
    compara el nombre SQL y el runtime usa `Integer`).
- **`@Size(max = 128)` en `titulo`** (y no 255): el esquema real verifica
  `titulo varchar(128)`; 255 admitiría valores que la BD rechazaría con 409.
- **Mensaje 404 con concordancia**: "Pelicula con id N **no encontrada**"
  (femenino, fijado en tests).
- **DELETE en un solo round-trip de carga**: `findById` + `delete(entity)`
  (entidad gestionada → un único `DELETE`), en lugar de `existsById` +
  `deleteById` (que hacía un `SELECT` extra).
- **`generoId`/`directorId` como `Long` simples**: sin relaciones JPA hacia
  `genero`/`director`, para no acoplar el microservicio a esos contextos.
  (Las FK de la BD real siguen aplicándose: id inexistente → 409.)
- **Starter web deprecado**: en Boot 4, `spring-boot-starter-web` está
  marcado deprecated en favor de `spring-boot-starter-webmvc`; se mantiene
  por coherencia con el proyecto (backlog transversal).
- **Tests web modularizados**: en Boot 4, MockMvc (`@AutoConfigureMockMvc`) y
  Jackson de pruebas viven en `spring-boot-starter-webmvc-test`
  (`scope=test`); sin él los tests no compilan.
- **Boot 4 sin `@MockBean`/`@MockitoBean`**: el 409 de integridad se prueba
  con MockMvc `standaloneSetup` + repository simulado (Mockito), sin contexto
  de Spring.
- **Jackson 3**: Boot 4 gestiona Jackson 3 (`tools.jackson.core`, paquetes
  `tools.jackson.*` en vez de `com.fasterxml.jackson.*`); el converter JSON de
  MVC es `JacksonJsonHttpMessageConverter` (Spring 7).
- **`open-in-view: false`**: las consultas JPA ocurren dentro de la capa de
  servicio, no en el controlador.
- **Semántica de errores**: `MethodArgumentTypeMismatchException` → 400
  (antes caía en el catch-all 500), `HttpRequestMethodNotSupportedException` →
  405, `HttpMediaTypeNotSupportedException` → 415,
  `DataIntegrityViolationException` → 409 (mensaje estable), y la búsqueda con
  `titulo` vacío → 400 (`@NotBlank` sobre el `@RequestParam`; en Spring 7 el
  fallo se reporta como `HandlerMethodValidationException`).
