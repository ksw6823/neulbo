#!/bin/bash

echo "🚀 Neulbo Backend 개발 환경 배포 시작..."

# 환경변수 파일 확인
if [ ! -f .env ]; then
    echo "❌ .env 파일이 없습니다. env.example을 참고해서 .env 파일을 생성해주세요."
    exit 1
fi

echo "🔨 Gradle 빌드 중..."
./gradlew clean bootJar

if [ $? -ne 0 ]; then
    echo "❌ Gradle 빌드 실패!"
    exit 1
fi

echo "🐳 Docker Compose로 서비스 시작 중..."
docker-compose down
if [ $? -ne 0 ]; then
    echo "❌ 기존 서비스 중지 실패!"
    exit 1
fi

docker-compose up -d --build
if [ $? -ne 0 ]; then
    echo "❌ Docker Compose 서비스 시작 실패!"
    echo "💡 다음 명령어로 상세 로그 확인: docker-compose logs"
    exit 1
fi

echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "🔍 서비스 상태 확인 중..."
docker-compose ps
if [ $? -ne 0 ]; then
    echo "❌ 서비스 상태 확인 실패!"
    echo "💡 Docker와 docker-compose가 설치되어 있고 실행 중인지 확인하세요."
    exit 1
fi

echo "🩺 헬스체크 실행 중..."
for i in {1..5}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ API 서버가 정상적으로 실행되었습니다!"
        echo ""
        echo "📋 서비스 정보:"
        echo "   - API 서버: http://localhost:8080"
        echo "   - PostgreSQL: localhost:5432"
        echo "   - Redis: localhost:6379"
        echo ""
        echo "🔗 테스트 URL:"
        echo "   - 구글 로그인: POST http://localhost:8080/oauth/login/google"
        echo "   - 카카오 로그인: POST http://localhost:8080/oauth/login/kakao"
        echo "   - 네이버 로그인: POST http://localhost:8080/oauth/login/naver"
        echo ""
        echo "📊 로그 확인: docker-compose logs -f api"
        exit 0
    fi
    echo "   시도 $i/5: API 서버 응답 대기 중..."
    sleep 10
done

echo "❌ API 서버가 시작되지 않았습니다. 로그를 확인해주세요:"
echo "   docker-compose logs api"
exit 1 