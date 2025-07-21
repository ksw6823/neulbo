# Neulbo Backend API

OAuth2 소셜 로그인을 지원하는 Spring Boot 백엔드 API 서버입니다.

## 🚀 빠른 시작

### 1. Ubuntu 환경 설정 (최초 1회)
```bash
# Ubuntu 환경 설정 스크립트 실행
chmod +x setup-ubuntu.sh
./setup-ubuntu.sh

# Docker 권한 적용 (필요시)
newgrp docker
# 또는 시스템 재부팅
```

### 2. 환경변수 설정
```bash
# env.example을 복사하여 .env 파일 생성
cp env.example .env

# .env 파일 편집
nano .env
# 또는
vim .env

# 실제 값들로 수정:
# - JWT_SECRET_KEY: 최소 32자 이상의 랜덤 문자열
# - OAuth2 클라이언트 ID/Secret들
```

### 3. 애플리케이션 실행

#### 🌐 배포/프로덕션 환경 (AWS RDS 사용)
```bash
# 프로덕션 배포 스크립트 실행
chmod +x deploy.sh
./deploy.sh

# Windows의 경우
deploy.bat
```

#### 💻 로컬 개발 환경 (로컬 PostgreSQL 사용)
```bash
# 로컬 개발 스크립트 실행
chmod +x dev.sh
./dev.sh

# Windows의 경우
dev.bat
```

### 4. 모니터링
```bash
# 서비스 상태 확인
chmod +x monitor.sh
./monitor.sh
```

## 📋 스크립트 구조

### 배포 스크립트
| 스크립트 | 환경 | 데이터베이스 | 용도 |
|---------|------|-------------|------|
| `deploy.sh` / `deploy.bat` | 프로덕션 | AWS RDS | 배포/운영 환경 |
| `dev.sh` / `dev.bat` | 로컬 | PostgreSQL 컨테이너 | 로컬 개발 |

### 주요 차이점
- **프로덕션 스크립트**: `docker-compose.prod.yml` 사용, AWS RDS 연결
- **개발 스크립트**: `docker-compose.yml` 사용, 로컬 PostgreSQL 컨테이너

### 서비스 포트
- **API 서버**: `http://localhost:8080`
- **PostgreSQL**: `localhost:5432` (개발 환경만)
- **Redis**: `localhost:6379`

## 🔄 코드 업데이트 방법

### 일반 업데이트 (서비스 중단)
```bash
# 실행 권한 부여
chmod +x update.sh

# 업데이트 실행
./update.sh
```

### 빠른 업데이트 (개발용)
```bash
# 실행 권한 부여
chmod +x quick-update.sh

# 빠른 업데이트 실행
./quick-update.sh
```

### 무중단 업데이트 (운영용)
```bash
# 실행 권한 부여
chmod +x zero-downtime-update.sh

# 무중단 업데이트 실행
./zero-downtime-update.sh
```

### 4. 수동 실행
```bash
# Gradle 빌드
./gradlew clean bootJar

# Docker Compose 실행
docker-compose -f docker-compose.dev.yml up -d --build

# 로그 확인
docker-compose -f docker-compose.dev.yml logs -f api
```

## 🔧 API 엔드포인트

### 인증
```bash
# 소셜 로그인 (구글, 카카오, 네이버) - 리액티브 방식 (권장)
POST http://localhost:8080/api/v1/oauth/login/{provider}
Body: {"code": "OAuth2_인증코드"}

# 소셜 로그인 - 블로킹 방식 (하위 호환성)
POST http://localhost:8080/api/v1/oauth/login/{provider}/blocking
Body: {"code": "OAuth2_인증코드"}

# 토큰 갱신
POST http://localhost:8080/auth/refresh
Header: Authorization: Bearer {refresh_token}

# 로그아웃
POST http://localhost:8080/auth/logout
Header: Authorization: Bearer {access_token}
```

### 헬스체크
```bash
GET http://localhost:8080/actuator/health
```

## 🛠️ 기술 스택

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Security + OAuth2**
- **JWT (Auth0)**
- **PostgreSQL 15**
- **Redis**
- **Docker + Docker Compose**

## 📊 서비스 정보

실행 후 다음 서비스들이 시작됩니다:

- **API 서버**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## 🐛 문제해결

### 서비스가 시작되지 않을 때
```bash
# 로그 확인
docker-compose -f docker-compose.dev.yml logs api

# 서비스 재시작
docker-compose -f docker-compose.dev.yml restart api

# 전체 재빌드
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml up -d --build
```

### 데이터베이스 초기화
```bash
# PostgreSQL 데이터 삭제 후 재시작
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

## 📱 플러터 앱 연동

플러터 앱에서 사용할 로그인 URL:
```dart
// 개발 환경 - 리액티브 API (권장)
final apiUrl = 'http://localhost:8080/api/v1/oauth/login/$provider';

// 개발 환경 - 블로킹 API (하위 호환성)
final apiUrlBlocking = 'http://localhost:8080/api/v1/oauth/login/$provider/blocking';

// 운영 환경 (AWS Lightsail)
final apiUrl = 'https://neulbo1.com/api/v1/oauth/login/$provider';
``` 