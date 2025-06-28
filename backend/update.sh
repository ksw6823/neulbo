#!/bin/bash

echo "🔄 Neulbo Backend 업데이트 시작..."

# Git에서 최신 코드 가져오기 (선택사항)
read -p "Git에서 최신 코드를 가져오시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📥 Git에서 최신 코드 가져오는 중..."
    git pull origin main
fi

echo "🔨 Gradle 빌드 중..."
./gradlew clean bootJar

if [ $? -ne 0 ]; then
    echo "❌ Gradle 빌드 실패!"
    exit 1
fi

echo "🛑 기존 컨테이너 중지 중..."
docker-compose -f docker-compose.dev.yml down

echo "🐳 새 이미지 빌드 및 컨테이너 시작 중..."
docker-compose -f docker-compose.dev.yml up -d --build

echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "🩺 헬스체크 실행 중..."
for i in {1..5}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 업데이트 완료! API 서버가 정상적으로 실행되었습니다!"
        echo ""
        echo "📊 서비스 상태 확인: ./monitor.sh"
        exit 0
    fi
    echo "   시도 $i/5: API 서버 응답 대기 중..."
    sleep 10
done

echo "❌ 업데이트 실패! 로그를 확인해주세요:"
echo "   docker-compose -f docker-compose.dev.yml logs api"
exit 1 