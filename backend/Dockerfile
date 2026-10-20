# syntax=docker/dockerfile:1

# --- Build : compile le jar avec le Maven Wrapper (aucun Maven requis sur l'hôte) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
# Dépendances dans une couche à part : elles ne sont re-téléchargées que si le pom change.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
# Les tests tournent en CI et en local (ils lancent eux-mêmes des conteneurs via Testcontainers).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# --- Runtime : JRE seul, utilisateur non root ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home regwatch
COPY --from=build /workspace/target/regwatch-backend-*.jar app.jar
USER regwatch

EXPOSE 8080
# La JVM dimensionne son tas d'après la limite mémoire du conteneur.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
