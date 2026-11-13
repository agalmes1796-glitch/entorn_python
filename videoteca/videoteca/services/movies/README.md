# movies

Microservicio de **películas** del proyecto *videoteca*. API REST CRUD sobre la
entidad `Movie` (tabla `movies`), persistida con Spring Data JPA en la base de
datos MariaDB local del proyecto.

## Stack

- **Java 25** (release 25 en build tooling)
- **Spring Boot 4.1.2** — el starter web es `spring-boot-starter-webmvc`
  (en Boot 4 el antiguo `spring-boot-starter-web` fue renombrado)
- **Spring Data JPA + Hibernate ORM** (entidad `Movie`)
- **Jakarta Bean Validation** (DTOs de entrada)
- **MariaDB** (runtime) / **H2 en modo MariaDB** (solo tests, `scope=test`)
- **Maven** con wrapper (`./mvnw`) — el proyecto es autocontenido

## Estructura

```
movies/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper/     # Maven Wrapper
└── src
    ├── main
    │   ├── java/com/videoteca/movies
    │   │   ├── MoviesServiceApplication.java
    │   │   ├── config/       # configuración (auditoría JPA)
    │   │   ├── controller/   # capa HTTP (solo translate HTTP ↔ service)
    │   │   ├── service/      # reglas de negocio
    │   │   ├── repository/   # Spring Data JPA
    │   │   ├── entity/       # entidad Movie (tabla movies)
    │   │   ├── dto/          # MovieRequest / MovieResponse / ErrorDetail
    │   │   └── exception/    # MovieNoEncontradaException + GlobalExceptionHandler
    │   └── resources/application.yml
    └── test/java/com/videoteca/movies  # tests de unidad (Mockito) + integración (H2)
```

## Requisitos

- **JDK 25** (comprobar con `java -version`)
- Docker para la base de datos local (o cualquier MariaDB 11.8 accesible)

## Cómo arrancar la base de datos

Desde la raíz del proyecto *videoteca* (donde está el `docker-compose.yaml`):

```bash
docker compose up -d
```

Esto levanta:

| Servicio      | URL / puerto                          | Credenciales                 |
|---------------|---------------------------------------|------------------------------|
| MariaDB 11.8  | `127.0.0.1:3306`, base `videoteca`    | `videoteca` / `videoteca_dev`|
| phpMyAdmin    | `http://127.0.0.1:8080`               | `videoteca` / `videoteca_dev`|

## Cómo ejecutar el microservice

```bash
cd movies

# Opción 1: directamente con Maven (wrapper)
./mvnw spring-boot:run

# Opción 2: empaquetar y ejecutar el jar
./mvnw verify
java -jar target/movies-0.0.1-SNAPSHOT.jar
```

El servicio escucha en **puerto 8082** (el 8081 ya lo usan
notifications/kafka/actors; 8080 es el gateway y 8761 Eureka).

Comprobación rápida de salud:

```bash
curl http://localhost:8082/actuator/health
# → {"status":"UP"}
```

## Endpoints

Base path: `/api/v1/movies`

| Método | Ruta                | Descripción                                             | Éxito | Errores |
|--------|---------------------|---------------------------------------------------------|-------|---------|
| GET    | `/api/v1/movies`    | Lista todas. Admite `?search=` (título, case-insensitive) | 200   | —       |
| GET    | `/api/v1/movies/{id}` | Detalle de una película                               | 200   | 404, 400 |
| POST   | `/api/v1/movies`    | Crea una película (cuerpo JSON)                         | 201 + `Location` | 400 |
| PUT    | `/api/v1/movies/{id}` | Sustitución completa de la película                  | 200   | 404, 400 |
| DELETE | `/api/v1/movies/{id}` | Elimina la película                                   | 204   | 404, 400 |

### Modelo (body de POST/PUT)

| Campo         | Tipo      | Obligatorio | Restricciones               |
|---------------|-----------|-------------|-----------------------------|
| `title`       | string    | sí          | 1–300 caracteres            |
| `director`    | string    | no          | hasta 200 caracteres        |
| `releaseYear` | integer   | sí          | 1895–2100                   |
| `genre`       | string    | no          | hasta 100 caracteres        |
| `description` | string    | no          | hasta 2000 caracteres       |
| `rating`      | number    | no          | 0–10, 2 decimales           |

La respuesta incluye además `id`, `createdAt` y `updatedAt` (mantenidos
automáticamente por JPA).

### Ejemplos curl

```bash
# Crear
curl -s -X POST http://localhost:8082/api/v1/movies \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "El Padrino",
    "director": "Francis Ford Coppola",
    "releaseYear": 1972,
    "genre": "Drama",
    "description": "Saga familiar de la mafia neoyorquina.",
    "rating": 8.50
  }'
# → 201, cabecera Location: http://localhost:8082/api/v1/movies/1

# Listar
curl -s http://localhost:8082/api/v1/movies

# Búsqueda por título (case-insensitive)
curl -s 'http://localhost:8082/api/v1/movies?search=padrino'

# Detalle
curl -s http://localhost:8082/api/v1/movies/1

# Sustitución completa (los campos omitidos se vacían)
curl -s -X PUT http://localhost:8082/api/v1/movies/1 \
  -H 'Content-Type: application/json' \
  -d '{ "title": "El Padrino II", "releaseYear": 1974 }'

# Eliminar
curl -s -X DELETE http://localhost:8082/api/v1/movies/1 -o /dev/null -w '%{http_code}\n'
# → 204

# Error 404 (formato estándar de error de la API)
curl -s http://localhost:8082/api/v1/movies/999
# → { "timestamp": "...", "status": 404, "error": "Not Found",
#      "path": "/api/v1/movies/999", "message": "Pelicula con id 999 no encontrada" }

# Error 400 (validación, con mapa campo → mensaje en "errors")
curl -s -X POST http://localhost:8082/api/v1/movies \
  -H 'Content-Type: application/json' \
  -d '{ "title": "", "releaseYear": 1890 }'
# → { "timestamp": "...", "status": 400, "error": "Bad Request",
#      "path": "/api/v1/movies", "message": "El cuerpo de la petición no es válido",
#      "errors": { "title": "El título es obligatorio", "releaseYear": "El año de estreno debe ser 1895 o posterior" } }
```

## Formato de errores

Todos los errores se devuelven con `Content-Type: application/json` y el mismo
cuerpo:

| Campo       | Tipo              | Descripción                                              |
|-------------|-------------------|----------------------------------------------------------|
| `timestamp` | string (ISO-8601) | Momento de la respuesta                                  |
| `status`    | int               | Código HTTP (400, 404, 415, 500, ...)                    |
| `error`     | string            | Razón estándar del estado (`Bad Request`, `Not Found`,…) |
| `path`      | string            | URI de la petición                                       |
| `message`   | string            | Mensaje legible del error                                |
| `errors`    | object o `null`   | Solo en 400 de validación: mapa campo → mensaje          |

Ejemplo de **400** (validación):

```json
{
  "timestamp": "2026-11-07T10:29:32.362630Z",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/v1/movies",
  "message": "El cuerpo de la petición no es válido",
  "errors": {
    "title": "El título es obligatorio",
    "releaseYear": "El año de estreno debe ser 1895 o posterior"
  }
}
```

Ejemplo de **404** (recurso no encontrado):

```json
{
  "timestamp": "2026-11-07T10:29:32.351714Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v1/movies/999",
  "message": "Pelicula con id 999 no encontrada",
  "errors": null
}
```

## Variables de entorno

Todo lo de `application.yml` es sobreescribible vía variables de entorno
(Spring lo resuelve nativamente; p. ej. `SPRING_DATASOURCE_URL` sustituye toda
la URL). Las variables soportadas explícitamente (con defaults) son:

| Variable        | Default         | Descripción                                  |
|-----------------|-----------------|----------------------------------------------|
| `DB_HOST`       | `localhost`     | Host de MariaDB                              |
| `DB_PORT`       | `3306`          | Puerto de MariaDB                            |
| `DB_NAME`       | `videoteca`     | Nombre de la base de datos                   |
| `DB_USER`       | `videoteca`     | Usuario de la BD                             |
| `DB_PASSWORD`   | `videoteca_dev` | Contraseña de la BD                          |
| `SERVER_PORT`   | `8082`          | Puerto HTTP del microservice (binding nativo)|

Ejemplo:

```bash
DB_HOST=192.168.1.50 DB_PASSWORD=otra_contrasena ./mvnw spring-boot:run
```

## Tests

Los tests **no necesitan Docker ni MariaDB**: usan el perfil `h2` (H2 en
memoria en modo compatibilidad MariaDB, `scope=test` en el pom).

```bash
cd movies
./mvnw test       # solo tests
./mvnw verify     # tests + empaquetado (jar bootable)
```

Cobertura mínima:

- **Unidad** (`service.MovieServiceTest`, Mockito): crear, listar, listar con
  búsqueda (incluye escapado de comodines LIKE), obtener, actualizar, borrar y
  los casos de no-encuentro → `MovieNoEncontradaException`.
- **Integración** (`MovieIntegrationTest`, aplicación completa + H2 +
  `TestRestTemplate`): happy paths POST/GET/PUT/DELETE, búsqueda con
  `?search=`, 404 (GET/PUT/DELETE), 400 con el formato de error definido,
  id no numérico en path, y `/actuator/health`.
