# 멀티스테이지 빌드로 최적화
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build --no-daemon -x test

# 실행 스테이지
FROM openjdk:21-jdk-slim

WORKDIR /app

# 필요한 패키지 설치 (healthcheck용)
RUN apt-get update && apt-get install -y wget curl && rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성 (보안)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 권한 설정
RUN chown appuser:appuser app.jar

# 사용자 전환
USER appuser

# 포트 노출
EXPOSE 8080

# JVM 옵션 최적화
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 