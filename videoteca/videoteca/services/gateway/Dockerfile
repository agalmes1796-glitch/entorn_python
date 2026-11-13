# ==============================================================================
# gateway — Spring Cloud Gateway reactivo / WebFlux (Spring Boot 4 / Java 25)
# Multi-etapa: build con Maven (JDK 25) -> runtime mínimo (JRE 25, usuario sin
# privilegios). La comprobación de salud se define en docker-compose.yaml
# (bash /dev/tcp: la imagen eclipse-temurin no incluye curl ni wget).
# ==============================================================================

# Etapa 1: compilación
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache de dependencias: primero se copia solo el pom para descargar el
# árbol de dependencias (capa estable entre builds).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código fuente y empaquetado del jar ejecutable (tests omitidos).
COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: runtime
FROM eclipse-temurin:25-jre
# Usuario sin privilegios para ejecutar la aplicación.
RUN useradd --create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
