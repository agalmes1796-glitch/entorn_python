# kafka

Microservicio de mensajería del proyecto **videoteca** basado en **Spring Boot 4** (4.0.9) y **Apache Kafka** (spring-kafka). Se registra en el servidor de **Eureka** de la stack y puede usarse a través del **API gateway** (`lb://kafka`).

- **API REST** para publicar mensajes: `POST /api/messages`
- **Producer** que publica en el topic `videoteca-events`
- **Consumer** (`@KafkaListener`) que registra en log cada mensaje recibido
- Imagen Docker multi-etapa (build con Maven, runtime con JRE 25 sin privilegios)

## Estructura

```
src/main/java/com/videoteca/kafka/
├── KafkaServiceApplication.java        # Punto de entrada
├── config/
│   ├── KafkaProperties.java            # Propiedades (bloque kafka: de application.yml)
│   └── KafkaConfig.java                # Beans de producer/consumer
├── producer/
│   └── MessageProducerService.java     # Publica mensajes vía KafkaTemplate
├── consumer/
│   └── MessageConsumerListener.java    # @KafkaListener del topic de eventos
└── controller/
    └── MessageController.java          # POST /api/messages
```

## Ejecución con Docker Compose (recomendado)

Desde la raíz del proyecto:

```bash
# Arranca solo broker + microservicio (o toda la stack):
docker compose up -d kafka
```

Luego publica un mensaje:

```bash
curl -X POST http://127.0.0.1:8082/api/messages \
     -H "Content-Type: application/json" \
     -d '{"payload": "Hola desde el microservicio Kafka"}'
```

Respuesta con el destino asignado (topic:partición):

```json
{"destination": "videoteca-events:0", "message": "Hola desde el microservicio Kafka"}
```

El listener del propio microservicio muestra en consola el mensaje recibido
(`docker compose logs -f kafka`).

También es accesible a través del gateway (puerto 127.0.0.1:8090):

```bash
curl -X POST http://127.0.0.1:8090/api/messages \
     -H "Content-Type: application/json" \
     -d '{"payload": "A través del gateway"}'
```

## Ejecución local (sin Docker)

1. Arranca el broker: `docker compose up -d kafka`
2. Compila y ejecuta:

   ```bash
   cd services/kafka
   mvn spring-boot:run
   ```

   Si Eureka corre en Docker, apúntale con
   `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/`.

## Configuración

| Propiedad | Variable de entorno | Valor por defecto |
|---|---|---|
| `kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `kafka.topic.events` | — | `videoteca-events` |
| `kafka.consumer.group-id` | — | `videoteca-kafka-group` |
| `eureka.client.service-url.defaultZone` | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka:8761/eureka/` |
| `server.port` | — | `8081` |
