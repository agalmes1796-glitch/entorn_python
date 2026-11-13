# Etapa build: compilació amb Maven i JDK 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Copia primer el POM per aprofitar la capa de dependències en el cache.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# Etapa runtime: JRE 25 mínim + curl per al healthcheck
FROM eclipse-temurin:25-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
# L'aplicació s'executa com a usuari no privilegiat (el healthcheck amb curl no necessita root).
RUN useradd -m app
WORKDIR /app
# El jar ja entra amb l'usuari 'app' (creat a dalt) a la capa correcta: una capa menys.
COPY --chown=app:app --from=build /build/target/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
