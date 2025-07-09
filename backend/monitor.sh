#!/bin/bash

echo "📊 Neulbo Backend 서비스 모니터링"
echo "=================================="

# Docker 서비스 상태 확인
echo "🐳 Docker 컨테이너 상태:"
docker-compose ps
if [ $? -ne 0 ]; then
    echo "❌ Docker Compose 상태 확인 실패! Docker와 docker-compose가 설치되어 있고 실행 중인지 확인하세요."
    exit 1
fi

echo ""
echo "💾 시스템 리소스 사용량:"
echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)% 사용 중"
echo "메모리: $(free -h | awk '/^Mem:/ {print $3 "/" $2}')"
echo "디스크: $(df -h | grep '^/dev/' | awk '{print $5}' | head -1) 사용 중"

echo ""
echo "🌐 네트워크 포트 상태:"
sudo netstat -tlnp | grep -E ":(8080|5432|6379)"

echo ""
echo "📋 최근 API 로그 (마지막 10줄):"
docker-compose logs --tail=10 api
if [ $? -ne 0 ]; then
    echo "❌ API 컨테이너 로그 조회 실패! 컨테이너가 실행 중인지 확인하세요."
    echo "💡 다음 명령어로 수동 확인: docker-compose ps"
fi

echo ""
echo "🩺 헬스체크:"
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ API 서버 정상"
    curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
else
    echo "❌ API 서버 비정상"
fi

echo ""
echo "🔄 실시간 로그 보기: docker-compose logs -f api"
echo "🛑 서비스 중지: docker-compose down"
echo "🔄 서비스 재시작: docker-compose restart" 