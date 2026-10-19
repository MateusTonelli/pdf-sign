FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 appuser
COPY --from=build /app/target/pdf-sign-*.jar app.jar
RUN mkdir -p /data/storage && chown -R appuser:appuser /app /data
USER appuser
ENV APP_STORAGE_PATH=/data/storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
