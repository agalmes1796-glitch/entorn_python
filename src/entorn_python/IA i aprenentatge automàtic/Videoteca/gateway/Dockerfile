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
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
