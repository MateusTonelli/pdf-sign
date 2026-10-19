FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/pdf-sign-*.jar /app/app.jar
RUN mkdir -p /data/storage
ENV APP_STORAGE_PATH=/data/storage
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=12 --start-period=90s \
  CMD curl -sf http://127.0.0.1:8080/actuator/health || exit 1
# Run as root so Coolify volume mounts are always writable
ENTRYPOINT ["java","-jar","/app/app.jar"]
