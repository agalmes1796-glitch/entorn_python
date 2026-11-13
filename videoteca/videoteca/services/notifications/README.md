# Microservicio de Notificaciones

Microservicio Spring Boot independiente (proyecto Maven propio, **fuera** del stack principal) que envía y recibe notificaciones genéricas:

- **Salientes (outbound):** envío por email vía SMTP (canal configurable).
- **Entrantes (inbound):** recepción de notificaciones de otros servicios (p. ej. `movies`).

Persistencia **en memoria** (`ConcurrentHashMap`, repositorios interfaz + implementación separada). Sin base de datos.

## Stack

| Componente | Versión |
|---|---|
| Spring Boot | **4.1.2** (estable) |
| Java | **25** (GraalVM CE 25.0.2) |
| Maven | 3.9.14 (con wrapper `mvnw`) |
| Jackson | 3.x (`tools.jackson.*`) |
| Servlet | Tomcat 11 (Jakarta EE 11) |

Paquete base: `com.videoteca.notifications` · Puerto: **8081**

## Cómo ejecutar

Requisitos: JDK 25+ (y Maven, o usar el wrapper incluido).

```bash
cd notifications

# Tests (suite completa: unidad + integración MockMvc)
./mvnw test

# Ejecutar en modo desarrollo
./mvnw spring-boot:run

# Empaquetar y ejecutar el jar
./mvnw -q package
java -jar target/notifications-0.0.1-SNAPSHOT.jar
```

El servicio arranca **sin necesidad de un servidor SMTP**:
`GET http://localhost:8081/actuator/health` → `{"status":"UP"}`.

## Variables de entorno

Relaxed binding de Spring sobre `application.yml` (sin secretos hardcoded):

| Variable | Por defecto | Descripción |
|---|---|---|
| `SPRING_MAIL_HOST` | `localhost` | Host del servidor SMTP |
| `SPRING_MAIL_PORT` | `25` | Puerto del servidor SMTP |
| `SPRING_MAIL_USERNAME` | *(vacío)* | Usuario (si el SMTP lo exige) |
| `SPRING_MAIL_PASSWORD` | *(vacío)* | Contraseña (si el SMTP lo exige) |
| `NOTIFICATIONS_STORE_MAX_SIZE` | `10000` | Tope de entradas por store en memoria (ver Limitaciones) |

Ejemplo:

```bash
SPRING_MAIL_HOST=smtp.mine.co SPRING_MAIL_PORT=587 \
SPRING_MAIL_USERNAME=noti SPRING_MAIL_PASSWORD=secreta \
java -jar target/notifications-0.0.1-SNAPSHOT.jar
```

## Endpoints

Base API: `/api/v1/notifications`

### Outbound

| Método y ruta | Descripción | Respuesta |
|---|---|---|
| `POST /outbound/send` | Envía una notificación por email | `202` incluso si SMTP falla (el registro queda `FAILED`) |
| `GET /outbound?status=SENT&limit=50` | Listado de resúmenes (recientes primero) | `200` |
| `GET /outbound/{id}` | Detalle completo | `200` / `404` JSON |
| `POST /outbound/{id}/resend` | Reintenta el envío | `200` / `404` / **`409`** si ya está `SENT` o hay un reenvío en curso |

`status` válido: `SENT` \| `FAILED`. `limit` entre 1 y 1000 (por defecto 100).

### Inbound

| Método y ruta | Descripción | Respuesta |
|---|---|---|
| `POST /inbound` | Registra una notificación entrante | `201` con `read=false` |
| `GET /inbound?read=true&type=X&limit=50` | Listado con filtros opcionales | `200` |
| `GET /inbound/{id}` | Detalle | `200` / `404` JSON |
| `PATCH /inbound/{id}/read` | Marca como leída/no leída (body `{"read":true}` **o** `?read=true`) | `200` / `404` |
| `DELETE /inbound/{id}` | Elimina | `204` / `404` |

En `PATCH /{id}/read`, si ambos se indican, **el body tiene precedencia sobre el parámetro** `?read=`.

### Actuator

| Método y ruta | Descripción |
|---|---|
| `GET /actuator/health` | Salud (solo `health` e `info` expuestos) |
| `GET /actuator/info` | Información del servicio |

## Ejemplos (curl)

Envío con payload de película (sin SMTP el registro queda `FAILED`, nunca hay 500, y el `errorMessage` es siempre un mensaje sanitizado):

```bash
curl -s -X POST http://localhost:8081/api/v1/notifications/outbound/send \
  -H 'Content-Type: application/json' \
  -d '{
    "to": "usuario@dominio.com",
    "cc": ["copias@dominio.com"],
    "subject": "Película añadida",
    "body": "La película Blade Runner fue añadida al catálogo.",
    "type": "MOVIE_ADDED",
    "referenceId": "123",
    "payload": { "movieId": 42, "title": "Blade Runner", "year": 1982 }
  }'
# → {"id":"...","status":"FAILED","to":"usuario@dominio.com","type":"MOVIE_ADDED","sentAt":null,"errorMessage":"Fallo de envío SMTP"}
```

Recepción entrante y marcado como leída:

```bash
# Crear (201)
curl -s -X POST http://localhost:8081/api/v1/notifications/inbound \
  -H 'Content-Type: application/json' \
  -d '{
    "sender": "movies",
    "type": "MOVIE_REMOVED",
    "subject": "Película retirada",
    "body": "La película 42 fue retirada del catálogo.",
    "referenceId": "42",
    "payload": { "movieId": 42, "title": "Blade Runner" }
  }'

# Marcar como leída por parámetro (200); el body gana si se pasa ambos
curl -s -X PATCH 'http://localhost:8081/api/v1/notifications/inbound/{id}/read?read=true'

# Listar solo leídas de un tipo
curl -s -G http://localhost:8081/api/v1/notifications/inbound \
  --data 'type=MOVIE_REMOVED' --data 'read=true'

# Eliminar (204)
curl -s -X DELETE http://localhost:8081/api/v1/notifications/inbound/{id} -o /dev/null -w '%{http_code}\n'
```

Reintento de envío (200 si era `FAILED`; **409** si ya está `SENT`):

```bash
curl -s -X POST http://localhost:8081/api/v1/notifications/outbound/{id}/resend
```

## Formatos de error

Todos los errores vuelven JSON consistente, sin stack traces ni detalles internos:

- **400** validación:
  ```json
  { "error": "Bad Request", "errors": [ { "field": "to", "message": "must not be blank" } ] }
  ```
  Cubren: `MethodArgumentNotValidException` (body), `HandlerMethodValidationException` (parámetros, p. ej. `limit=0` o `limit=1001`), `HttpMessageNotReadableException` (JSON malformado, con **mensaje fijo** sin reflejar el detalle de Jackson), `MethodArgumentTypeMismatchException` (UUID inválido) y errores de dominio `BadRequestException`.
- **404**:
  ```json
  { "error": "Not Found", "message": "Notificación saliente no encontrada: ...", "path": "/api/v1/..." }
  ```
- **409** conflicto (reenvío de una notificación ya `SENT`, o reenvío concurrente en curso):
  ```json
  { "error": "Conflict", "message": "La notificación ya fue enviada: ...", "path": "/api/v1/..." }
  ```
- **500** genérico y seguro (solo `error: "Internal Server Error"`, el detalle se loggea, nunca se expone).

Gestiona `GlobalExceptionHandler` (`@RestControllerAdvice`).

## Validación de peticiones (topes de tamaño)

Outbound (`POST /outbound/send`):

| Campo | Restricciones |
|---|---|
| `to` | obligatorio, email válido |
| `cc` | opcional, emails válidos, **máx. 20** destinatarios |
| `subject` | obligatorio, **máx. 200** caracteres |
| `body` | obligatorio, **máx. 20 000** caracteres |
| `type` | obligatorio, patrón `[A-Z0-9_]{2,50}` |
| `referenceId` | opcional, **máx. 100** caracteres |
| `payload` | objeto JSON opcional |

Inbound (`POST /inbound`):

| Campo | Restricciones |
|---|---|
| `sender` | obligatorio, patrón `[A-Za-z0-9._-]{1,100}` |
| `type` | obligatorio, patrón `[A-Z0-9_]{2,50}` |
| `subject` | obligatorio, **máx. 200** caracteres |
| `body` | obligatorio, **máx. 20 000** caracteres |
| `referenceId` | opcional, **máx. 100** caracteres |
| `payload` | objeto JSON opcional |

Query params: `limit` entre **1 y 1000** en ambos listados (400 si se sale del rango).

## Limitaciones conocidas (por diseño)

- **Persistencia en memoria con crecimiento acotado:** al reiniciar el servicio se pierde todo. Cada store (inbound y outbound) retiene como máximo `notifications.store.max-size` entradas (por defecto **10 000**); al llegar al tope, el guardado de una entrada nueva **evicta la más antigua** (por `createdAt`/`receivedAt`) y lo loguea. Pensado para desarrollo/demos; los repositorios son interfaces, por lo que un backend real (p. ej. JPA) encajaría sin tocar el servicio.
- **Sin SMTP, el envío no falla el servicio:** `POST /outbound/send` devuelve `202` y el registro pasa a `status=FAILED`. El `errorMessage` persistido/exposto es **siempre sanitizado** (`Fallo de autenticación SMTP`, `Fallo de envío SMTP` o `Error interno al enviar la notificación`): nunca se expone el detalle crudo de JavaMail (host:puerto, respuestas del servidor); ese detalle queda solo en los logs.
- **Reenvíos acotados:** `resend` sobre un registro `SENT` devuelve 409 (no reenvía) y existe una guarda de carrera que impide la doble entrega concurrente de un mismo reenvío (también 409).
- **Privacidad en logs:** los emails de los logs de envío van enmascarados (p. ej. `p***@videoteca.local`); `type` y `sender` quedan blindados por sus patrones de validación.
- **El health no depende del mail** (`management.health.mail.enabled: false`, ya que en Boot 4 hace `testConnection()` real).
- **Sin autenticación/autorización** ni paginación (solo `limit` ≤ 1000).

## Estructura

```
notifications/
├── pom.xml
├── .gitignore
├── mvnw  mvnw.cmd  .mvn/wrapper/
├── README.md
└── src
    ├── main
    │   ├── java/com/videoteca/notifications
    │   │   ├── NotificationsApplication.java
    │   │   ├── api
    │   │   │   ├── OutboundNotificationController.java
    │   │   │   ├── InboundNotificationController.java
    │   │   │   ├── dto/            # OutboundSendRequest/Response, OutboundSummary, InboundCreateRequest, InboundReadRequest
    │   │   │   └── web/            # GlobalExceptionHandler (400/404/409/500) + DTOs de error
    │   │   ├── domain
    │   │   │   ├── OutboundNotification.java   # inmutable, withSentAt/withFailure
    │   │   │   ├── InboundNotification.java    # inmutable, withRead
    │   │   │   ├── NotificationStatus.java
    │   │   │   ├── exception/      # BadRequest, Conflict, NotificationNotFound
    │   │   │   └── repository      # interfaces + inmemory/ (ConcurrentHashMap acotado con eviction)
    │   │   ├── service
    │   │   │   ├── OutboundNotificationService.java  # envío SMTP try/catch → FAILED sanitizado, 409 resend
    │   │   │   └── InboundNotificationService.java
    │   │   └── util/EmailMasker.java            # enmascarado de emails para logs
    │   └── resources/application.yml
    └── test/java/com/videoteca/notifications
        ├── api/NotificationsApiIntegrationTest.java        # MockMvc: todos los endpoints + 409/400/404 nuevos
        ├── service/InboundNotificationServiceTest.java
        ├── service/OutboundNotificationServiceTest.java     # sanitización, 409 resend, carrera in-flight
        └── domain/repository/inmemory
            ├── InMemoryInboundNotificationRepositoryTest.java
            └── InMemoryOutboundNotificationRepositoryTest.java
```

## Tests

- **Unidad** (51): repositorios en memoria (incluida la **eviction** al llegar al tope) y servicios, con el `JavaMailSender` mockeado (Mockito); cubren también la sanitización del `errorMessage` y la guarda de carrera del `resend`.
- **Integración** (31): `@SpringBootTest` + MockMvc contra **todos** los endpoints, con `JavaMailSender` sustituido por un mock (`@MockitoBean`); nunca se toca un SMTP real. Cubren el 409 de `resend`, la ruta inexistente → 404 JSON, y los 400 por tamaño (`cc`, `subject`, `body`, `limit=1001`).
- Total: **82 tests en verde** con `./mvnw test`.
