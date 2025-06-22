#!/bin/bash

echo "🔄 Neulbo Backend 무중단 업데이트 시작..."

# 현재 실행 중인 서비스 확인
if docker-compose -f docker-compose.blue-green.yml ps | grep -q "api-blue.*Up"; then
    CURRENT="blue"
    TARGET="green"
    CURRENT_PORT="8080"
    TARGET_PORT="8081"
else
    CURRENT="green"
    TARGET="blue"
    CURRENT_PORT="8081"
    TARGET_PORT="8080"
fi

echo "📊 현재 실행 중: api-$CURRENT (포트 $CURRENT_PORT)"
echo "🎯 배포 대상: api-$TARGET (포트 $TARGET_PORT)"

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

echo "🐳 새로운 컨테이너($TARGET) 빌드 및 시작 중..."
docker-compose -f docker-compose.blue-green.yml --profile $TARGET up -d --build api-$TARGET

echo "⏳ 새 서비스 시작 대기 중..."
sleep 30

echo "🩺 새 서버 헬스체크 중..."
for i in {1..10}; do
    if curl -f http://localhost:$TARGET_PORT/actuator/health > /dev/null 2>&1; then
        echo "✅ 새 서버(api-$TARGET) 정상 시작!"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "❌ 새 서버 시작 실패! 롤백 중..."
        docker-compose -f docker-compose.blue-green.yml stop api-$TARGET
        exit 1
    fi
    echo "   시도 $i/10: 새 서버 응답 대기 중..."
    sleep 10
done

echo "🔄 트래픽 전환 준비..."
echo "새 서버가 정상적으로 시작되었습니다."
echo "현재 서버: http://localhost:$CURRENT_PORT"
echo "새 서버: http://localhost:$TARGET_PORT"

read -p "트래픽을 새 서버로 전환하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🔄 포트 전환 중..."
    
    # 현재 서버 중지
    docker-compose -f docker-compose.blue-green.yml stop api-$CURRENT
    
    # 새 서버를 기본 포트(8080)로 변경
    if [ "$TARGET" = "green" ]; then
        docker-compose -f docker-compose.blue-green.yml stop api-green
        # 포트 매핑 변경을 위해 재시작
        sed -i 's/8081:8080/8080:8080/' docker-compose.blue-green.yml
        docker-compose -f docker-compose.blue-green.yml --profile green up -d api-green
        # 원래 설정으로 복원
        sed -i 's/8080:8080/8081:8080/' docker-compose.blue-green.yml
    fi
    
    echo "✅ 무중단 업데이트 완료!"
    echo "🗑️  이전 컨테이너 정리: docker-compose -f docker-compose.blue-green.yml rm api-$CURRENT"
else
    echo "❌ 업데이트 취소. 새 서버를 중지합니다."
    docker-compose -f docker-compose.blue-green.yml stop api-$TARGET
fi 