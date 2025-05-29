# 1단계: 빌드용 Gradle 이미지
FROM gradle:8.4-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar

# 2단계: 실행용 OpenJDK 슬림 이미지
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
