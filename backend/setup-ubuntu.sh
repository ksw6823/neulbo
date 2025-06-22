#!/bin/bash

echo "🐧 Ubuntu 환경 설정 시작..."

# 시스템 업데이트
echo "📦 시스템 패키지 업데이트 중..."
sudo apt update

# 필수 패키지 설치
echo "🔧 필수 패키지 설치 중..."
sudo apt install -y curl wget git

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "🐳 Docker 설치 중..."
    
    # Docker의 공식 GPG 키 추가
    sudo apt install -y ca-certificates gnupg lsb-release
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    
    # Docker 저장소 추가
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # Docker 설치
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # 현재 사용자를 docker 그룹에 추가
    sudo usermod -aG docker $USER
    
    echo "⚠️  Docker 설치 완료! 다음 명령어로 다시 로그인하거나 재부팅하세요:"
    echo "   newgrp docker"
    echo "   또는 시스템 재부팅"
else
    echo "✅ Docker가 이미 설치되어 있습니다."
fi

# Docker Compose 설치 확인
if ! command -v docker-compose &> /dev/null; then
    echo "📝 Docker Compose 설치 중..."
    sudo apt install -y docker-compose
else
    echo "✅ Docker Compose가 이미 설치되어 있습니다."
fi

# Java 17+ 설치 확인 (Gradle 실행용)
if ! command -v java &> /dev/null; then
    echo "☕ OpenJDK 21 설치 중..."
    sudo apt install -y openjdk-21-jdk
else
    echo "✅ Java가 이미 설치되어 있습니다."
fi

# 방화벽 설정 (필요시)
echo "🔥 방화벽 포트 열기..."
sudo ufw allow 8080/tcp
sudo ufw allow 5432/tcp
sudo ufw allow 6379/tcp

echo "✅ Ubuntu 환경 설정 완료!"
echo ""
echo "📋 다음 단계:"
echo "1. 환경변수 설정: cp env.example .env && nano .env"
echo "2. 애플리케이션 실행: ./deploy.sh"
echo ""
echo "⚠️  만약 Docker 권한 오류가 발생하면:"
echo "   newgrp docker"
echo "   또는 시스템 재부팅 후 다시 시도하세요." 