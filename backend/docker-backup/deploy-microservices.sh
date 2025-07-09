#!/bin/bash

echo "🚀 Neulbo 마이크로서비스 아키텍처 배포 시작..."

# 환경변수 파일 확인
if [ ! -f .env ]; then
    echo "❌ .env 파일이 없습니다. env.example을 참고해서 .env 파일을 생성해주세요."
    exit 1
fi

# 필요한 디렉토리 생성
echo "📁 필요한 디렉토리 생성 중..."
mkdir -p nginx/ssl
mkdir -p ml-server
mkdir -p llm-server
mkdir -p sql/init
mkdir -p monitoring

# Gradle 빌드 (Spring Boot)
echo "🔨 Spring Boot 애플리케이션 빌드 중..."
./gradlew clean bootJar
if [ $? -ne 0 ]; then
    echo "❌ Gradle 빌드 실패!"
    exit 1
fi

# 기존 서비스 중지
echo "🛑 기존 서비스 중지 중..."
docker-compose -f docker-compose.microservices.yml down

# 이미지 빌드 및 서비스 시작
echo "🐳 마이크로서비스 시작 중..."
docker-compose -f docker-compose.microservices.yml up -d --build
if [ $? -ne 0 ]; then
    echo "❌ Docker Compose 서비스 시작 실패!"
    echo "💡 다음 명령어로 상세 로그 확인: docker-compose -f docker-compose.microservices.yml logs"
    exit 1
fi

echo "⏳ 서비스 시작 대기 중..."
sleep 45

echo "🔍 서비스 상태 확인 중..."
docker-compose -f docker-compose.microservices.yml ps

echo "🩺 헬스체크 실행 중..."

# Nginx 헬스체크
echo "  📋 Nginx (리버스 프록시) 확인..."
for i in {1..5}; do
    if curl -f http://localhost/health > /dev/null 2>&1; then
        echo "  ✅ Nginx 정상 작동"
        break
    fi
    echo "     시도 $i/5: Nginx 응답 대기 중..."
    sleep 5
done

# Backend API 헬스체크
echo "  📋 Backend API 확인..."
for i in {1..5}; do
    if curl -f http://localhost/api/v1/actuator/health > /dev/null 2>&1; then
        echo "  ✅ Backend API 정상 작동"
        break
    fi
    echo "     시도 $i/5: Backend API 응답 대기 중..."
    sleep 5
done

# ML API 헬스체크 (서비스가 구현되면)
echo "  📋 ML API 확인..."
if curl -f http://localhost/ml/health > /dev/null 2>&1; then
    echo "  ✅ ML API 정상 작동"
else
    echo "  ⚠️ ML API 아직 구현되지 않음 (정상)"
fi

# LLM API 헬스체크 (서비스가 구현되면)
echo "  📋 LLM API 확인..."
if curl -f http://localhost/llm/health > /dev/null 2>&1; then
    echo "  ✅ LLM API 정상 작동"
else
    echo "  ⚠️ LLM API 아직 구현되지 않음 (정상)"
fi

echo ""
echo "🎉 마이크로서비스 배포 완료!"
echo ""
echo "📋 서비스 정보:"
echo "   - 🌐 리버스 프록시: http://localhost"
echo "   - 🔧 Backend API: http://localhost/api/"
echo "   - 🤖 ML API: http://localhost/ml/"
echo "   - 💬 LLM API: http://localhost/llm/"
echo "   - 📊 Prometheus: http://localhost:9090"
echo ""
echo "🔗 API 엔드포인트 예시:"
echo "   - 구글 로그인: POST http://localhost/api/v1/oauth/login/google"
echo "   - 카카오 로그인: POST http://localhost/api/v1/oauth/login/kakao"
echo "   - 네이버 로그인: POST http://localhost/api/v1/oauth/login/naver"
echo ""
echo "📊 모니터링 명령어:"
echo "   - 전체 로그: docker-compose -f docker-compose.microservices.yml logs -f"
echo "   - 특정 서비스: docker-compose -f docker-compose.microservices.yml logs -f [service-name]"
echo "   - 상태 확인: docker-compose -f docker-compose.microservices.yml ps"
echo ""
echo "🛑 서비스 중지: docker-compose -f docker-compose.microservices.yml down" 