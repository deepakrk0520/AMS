# Multi-stage build for shopsphere-api (Java 21, matches pom.xml).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home shopsphere \
    && mkdir /app /logs && chown shopsphere /app /logs
USER shopsphere
WORKDIR /app
COPY --from=build --chown=shopsphere /build/target/shopsphere-api-*.jar app.jar
ENV LOG_DIR=/logs
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
