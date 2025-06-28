#!/bin/bash

echo "📊 Neulbo Backend 서비스 모니터링"
echo "=================================="

# Docker 서비스 상태 확인
echo "🐳 Docker 컨테이너 상태:"
docker-compose -f docker-compose.dev.yml ps

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
docker-compose -f docker-compose.dev.yml logs --tail=10 api

echo ""
echo "🩺 헬스체크:"
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ API 서버 정상"
    curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
else
    echo "❌ API 서버 비정상"
fi

echo ""
echo "🔄 실시간 로그 보기: docker-compose -f docker-compose.dev.yml logs -f api"
echo "🛑 서비스 중지: docker-compose -f docker-compose.dev.yml down"
echo "🔄 서비스 재시작: docker-compose -f docker-compose.dev.yml restart" 