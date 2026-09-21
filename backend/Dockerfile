# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Cache dependencies separately from source so code-only changes don't re-download the world.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S joblens && adduser -S joblens -G joblens
COPY --from=build /workspace/target/joblens-backend-*.jar app.jar
USER joblens

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
