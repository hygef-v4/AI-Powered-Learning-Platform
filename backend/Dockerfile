# Một image cho cả backend và worker (worker chạy với SPRING_PROFILES_ACTIVE=worker).
FROM maven:3.9-eclipse-temurin-17@sha256:1a352420f7aba21f5ad08df31bab55f74c013fb491f1ae8ab1dd7ff9ed698584 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine@sha256:e2ba4e947bd5e236a9866fbdda03e4936036cb9e9d16ea0d84d103d6634a5c21
# postgresql16-client cho pg_basebackup của worker (backup U03).
# Tạo sẵn thư mục mount volume tệp thuộc app: volume mới nhận quyền này, app ghi được.
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache wget postgresql16-client \
    && mkdir -p /tmp/uploads /tmp/backup /app/data/files \
    && chown -R app:app /tmp/uploads /tmp/backup /app/data
WORKDIR /app
COPY --from=build /src/target/platform-backend-*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health || exit 1
# G1 cố định: container dưới 1.792 MB RAM thì JVM tự chọn SerialGC, Full GC dừng cả app tới ~0,9 s khi nhiều người
# cùng lưu bài lớn (lệnh Redis 500 ms hết hạn, trả 503); G1 dừng tối đa ~0,24 s (TC-PERF-004, KAN-244).
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
