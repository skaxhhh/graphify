# syntax=docker/dockerfile:1

# 1) Build stage — Gradle wrapper로 실행 가능한 boot jar 생성
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐싱을 위해 빌드 스크립트/래퍼 먼저 복사
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew --version --no-daemon

# 소스 복사 후 테스트 제외하고 boot jar 빌드 (테스트는 CI/로컬에서 수행)
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# 2) Runtime stage — JRE만 포함한 경량 이미지
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 비루트 사용자로 실행
RUN groupadd --system graphify && useradd --system --gid graphify graphify
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER graphify

# Cloud Run이 PORT 환경변수를 주입 (기본 8080). server.port=${PORT:8080} 로 수신
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
