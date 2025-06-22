FROM openjdk:21-jdk-slim

WORKDIR /app

# Gradle 빌드 결과물 복사
COPY build/libs/*.jar app.jar

# 애플리케이션 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 