#!/bin/bash

echo "⚡ 빠른 업데이트 (개발용)"

echo "🔨 Gradle 빌드만 실행 중..."
./gradlew bootJar

if [ $? -ne 0 ]; then
    echo "❌ Gradle 빌드 실패!"
    exit 1
fi

echo "🔄 API 컨테이너만 재시작 중..."
docker-compose -f docker-compose.dev.yml restart api

echo "⏳ 서비스 재시작 대기 중..."
sleep 15

echo "🩺 헬스체크..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 빠른 업데이트 완료!"
else
    echo "❌ 서비스 시작 실패, 로그 확인: docker-compose -f docker-compose.dev.yml logs api"
fi 