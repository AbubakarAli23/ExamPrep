# syntax=docker/dockerfile:1

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependency resolution separately from source changes.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system --gid 1001 examprep \
    && useradd --system --uid 1001 --gid examprep --home-dir /app --shell /usr/sbin/nologin examprep

WORKDIR /app

COPY --from=build /build/target/examprep-1.0.0.jar /app/app.jar

# Uploaded documents live here and are mounted as a persistent volume.
RUN mkdir -p /app/data/uploads && chown -R examprep:examprep /app

USER examprep

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Configuration comes from the environment at runtime; no secrets are baked into the image.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
