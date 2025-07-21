# 🐳 Docker 배포 가이드

## 📁 스크립트 구조

### 🌐 프로덕션/배포 환경
```bash
# Linux/Mac
./deploy.sh

# Windows
deploy.bat
```
- **Docker Compose 파일**: `docker-compose.prod.yml`
- **데이터베이스**: AWS RDS
- **프로필**: `production`

### 💻 로컬 개발 환경
```bash
# Linux/Mac
./dev.sh

# Windows
dev.bat
```
- **Docker Compose 파일**: `docker-compose.yml`
- **데이터베이스**: PostgreSQL 컨테이너
- **프로필**: `local`

## 🔧 환경변수 설정

### 필수 환경변수 (.env 파일)
```env
# JWT 시크릿 키
JWT_SECRET_KEY=your-secret-key-at-least-32-chars

# 데이터베이스 (프로덕션용)
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/neulbo
DB_USERNAME=your-username
DB_PASSWORD=your-password

# OAuth2 클라이언트 정보
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret
NAVER_CLIENT_ID=your-naver-client-id
NAVER_CLIENT_SECRET=your-naver-client-secret
```

## 📋 Docker Compose 파일 비교

### docker-compose.yml (개발용)
- PostgreSQL 컨테이너 포함
- 모든 포트 외부 노출 (개발 편의성)
- 로컬 데이터베이스 사용

### docker-compose.prod.yml (프로덕션용)
- PostgreSQL 컨테이너 없음 (AWS RDS 사용)
- Redis만 내부 네트워크
- 리소스 제한 설정
- 로그 관리 설정

## 🚀 배포 명령어

### 프로덕션 배포
```bash
# 현재 서비스 중지
docker-compose -f docker-compose.prod.yml down

# 새 버전 배포
docker-compose -f docker-compose.prod.yml up -d --build
```

### 로컬 개발
```bash
# 현재 서비스 중지
docker-compose down

# 개발 환경 시작
docker-compose up -d --build
```

## 🔍 모니터링

### 서비스 상태 확인
```bash
# 프로덕션
docker-compose -f docker-compose.prod.yml ps

# 개발
docker-compose ps
```

### 로그 확인
```bash
# 프로덕션
docker-compose -f docker-compose.prod.yml logs -f api

# 개발
docker-compose logs -f api
```

### 헬스체크
```bash
curl http://localhost:8080/actuator/health
```

## ⚠️ 주의사항

1. **환경변수**: 프로덕션에서는 반드시 `.env` 파일 설정 필요
2. **포트 충돌**: 기존 PostgreSQL/Redis 서비스와 포트 충돌 주의
3. **메모리 사용량**: 프로덕션에서는 리소스 제한 설정됨
4. **보안**: 프로덕션에서는 Redis 포트 외부 노출 안함

## 🛠️ 트러블슈팅

### 일반적인 문제

1. **포트 이미 사용 중**
   ```bash
   # 기존 프로세스 확인
   netstat -tulpn | grep :8080
   
   # 프로세스 종료
   sudo kill -9 <PID>
   ```

2. **Docker 권한 문제**
   ```bash
   # Docker 그룹에 사용자 추가
   sudo usermod -aG docker $USER
   newgrp docker
   ```

3. **메모리 부족**
   ```bash
   # 사용하지 않는 컨테이너/이미지 정리
   docker system prune -a
   ```

4. **데이터베이스 연결 실패**
   - AWS RDS 보안 그룹 설정 확인
   - 환경변수 값 확인
   - 네트워크 연결 상태 확인 