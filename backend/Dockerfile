# Một image cho cả `backend` và `worker` (worker chạy với SPRING_PROFILES_ACTIVE=worker).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache wget \
    && mkdir -p /tmp/uploads && chown app:app /tmp/uploads
WORKDIR /app
COPY --from=build /src/target/platform-backend-*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health/ready || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
