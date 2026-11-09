# syntax=docker/dockerfile:1
#
# Microservicio Movies (Videoteca) — imagen multi-stage:
#   Etapa 1: compila el jar con Maven 3.10 + JDK 25 (Eclipse Temurin).
#   Etapa 2: runtime mínimo con JRE 25 (Eclipse Temurin) + curl para el
#            healthcheck, usuario no root.
#
# Build:
#   docker build -t videoteca-movies:1.0.0 .
# Run (la BD debe estar en 127.0.0.1:3306, p. ej. docker compose up -d en la raíz):
#   docker run --rm --name videoteca-movies \
#     -p 127.0.0.1:8084:8084 \
#     -e MOVIES_DB_URL=jdbc:mariadb://host.docker.internal:3306/videoteca \
#     videoteca-movies:1.0.0
#
# Nota: en docker-compose.yaml el contenedor escucha en 8080 (como el resto de
# microservicios, vía MOVIES_SERVER_PORT) y el host expone el 8084:
# "127.0.0.1:8084:8080".

# ---------- Etapa 1: build ----------
FROM maven:3.10.0-eclipse-temurin-25 AS build
WORKDIR /build

# Primero solo el POM para aprovechar la caché de dependencias.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B dependency:go-offline

# Después el código fuente y el empaquetado (se saltan los tests en la imagen).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests package

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# curl: lo usa el healthcheck del docker-compose.yaml.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Usuario de solo lectura para ejecutar la aplicación.
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build --chown=app:app /build/target/app.jar /app/app.jar

USER app
# Puerto por defecto de la app (ver application.yml); el compose lo fija a 8080
# dentro del contenedor con MOVIES_SERVER_PORT y expone el 8084 al host.
EXPOSE 8084
ENV JAVA_OPTS=""
# -XX:MaxRAMPercentage: escala la JVM con la memoria del contenedor.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -XX:MaxRAMPercentage=75 -jar /app/app.jar"]
