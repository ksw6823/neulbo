# 🐳 Neulbo Backend Docker 가이드

## 📁 파일 구조 (단순화됨)

```
backend/
├── Dockerfile                    # 프로덕션용 멀티스테이지 빌드
├── docker-compose.yml           # 개발환경 (기본)
├── docker-compose.prod.yml      # 프로덕션 환경
├── deploy.sh                    # Linux 배포 스크립트
├── deploy.bat                   # Windows 배포 스크립트  
├── monitor.sh                   # 모니터링 스크립트
├── env.example                  # 환경변수 예시
└── docker-backup/               # 백업된 복잡한 설정들
    ├── docker-compose.microservices.yml
    ├── docker-compose.blue-green.yml
    └── ...
```

## 🚀 빠른 시작

### 1. 환경변수 설정
```bash
cp env.example .env
# .env 파일을 편집하여 실제 값들 입력
```

### 2. 개발환경 시작
```bash
# Linux/Mac
./deploy.sh

# Windows
deploy.bat
```

### 3. 상태 확인
```bash
./monitor.sh
```

## 🛠️ 주요 명령어

### 개발환경 (기본)
```bash
# 서비스 시작
docker-compose up -d

# 서비스 중지
docker-compose down

# 로그 확인
docker-compose logs -f api

# 상태 확인
docker-compose ps
```

### 프로덕션 환경
```bash
# 프로덕션 배포
docker-compose -f docker-compose.prod.yml up -d --build

# 프로덕션 중지
docker-compose -f docker-compose.prod.yml down
```

## 📊 서비스 정보

### 개발환경
- **API 서버**: http://localhost:8080
- **PostgreSQL**: localhost:5432 (외부 접근 가능)
- **Redis**: localhost:6379 (외부 접근 가능)

### 프로덕션 환경
- **API 서버**: http://localhost:8080
- **PostgreSQL**: 내부 네트워크만
- **Redis**: 내부 네트워크만 (보안)

## 🔍 주요 엔드포인트

- **헬스체크**: GET http://localhost:8080/actuator/health
- **구글 로그인**: POST http://localhost:8080/api/v1/oauth/login/google
- **카카오 로그인**: POST http://localhost:8080/api/v1/oauth/login/kakao
- **네이버 로그인**: POST http://localhost:8080/api/v1/oauth/login/naver

## 🧹 단순화된 이유

이전에는 6개의 compose 파일과 8개의 배포 스크립트가 있어서 복잡했습니다:

**이전 (복잡):**
- docker-compose.dev.yml
- docker-compose.dev-hotreload.yml
- docker-compose.prod.yml
- docker-compose.rds.yml
- docker-compose.microservices.yml
- docker-compose.blue-green.yml

**현재 (단순):**
- docker-compose.yml (개발환경)
- docker-compose.prod.yml (프로덕션)

## 🔄 확장시 백업 파일 활용

나중에 마이크로서비스나 무중단 배포가 필요하면:

```bash
# 백업에서 복원
cp docker-backup/docker-compose.microservices.yml .
cp docker-backup/deploy-microservices.sh .
```

## 🚨 트러블슈팅

### 포트 충돌
```bash
# 포트 사용 확인
netstat -tlnp | grep :8080

# 기존 컨테이너 정리
docker-compose down --remove-orphans
```

### 로그 확인
```bash
# 전체 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs -f api
docker-compose logs -f postgres
docker-compose logs -f redis
```

## 📞 지원

문제가 있으면 다음 순서로 확인:
1. `docker-compose ps` - 컨테이너 상태
2. `docker-compose logs api` - API 로그
3. `./monitor.sh` - 전체 상태 확인 