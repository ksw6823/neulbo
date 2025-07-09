# 🚀 Neulbo Backend API 명세서

## 📋 개요
Spring Boot 3.x 기반의 OAuth2 인증을 지원하는 RESTful API 서버입니다.

**기본 URL**: `http://localhost:8080`  
**버전**: v1  
**인증 방식**: JWT Bearer Token  

---

## 🔐 인증 시스템

### OAuth 로그인 API

#### 1. OAuth 로그인 (리액티브 방식 - 권장)
```http
POST /api/v1/oauth/login/{provider}
```

**지원 Provider**: `google`, `kakao`, `naver`

**Request Body**:
```json
{
  "code": "OAuth_authorization_code"
}
```

**Request Body 검증**:
- `code`: 필수, 1-2000자 사이

**Response**:
```json
{
  "accessToken": "JWT_ACCESS_TOKEN",
  "refreshToken": "JWT_REFRESH_TOKEN", 
  "isNewUser": true
}
```

**Status Codes**:
- `200 OK`: 로그인 성공
- `400 Bad Request`: 잘못된 요청 (지원하지 않는 provider, 잘못된 code)
- `500 Internal Server Error`: OAuth 토큰 요청 실패

---

#### 2. OAuth 로그인 (블로킹 방식 - 하위 호환성)
```http
POST /api/v1/oauth/login/{provider}/blocking
```

**⚠️ Deprecated**: 리액티브 방식 사용을 권장합니다.

**Request/Response**: 위와 동일

---

### 토큰 관리 API

#### 3. 액세스 토큰 갱신
```http
POST /auth/refresh
```

**Headers**:
```
Authorization: Bearer {REFRESH_TOKEN}
```

**Response**:
```json
{
  "accessToken": "NEW_JWT_ACCESS_TOKEN"
}
```

**Status Codes**:
- `200 OK`: 토큰 갱신 성공
- `400 Bad Request`: 리프레시 토큰 누락
- `401 Unauthorized`: 유효하지 않은 리프레시 토큰

---

#### 4. 로그아웃
```http
POST /auth/logout
```

**Headers**:
```
Authorization: Bearer {ACCESS_TOKEN}
```

**Response**:
```json
"로그아웃 성공"
```

**Status Codes**:
- `200 OK`: 로그아웃 성공
- `400 Bad Request`: 액세스 토큰 누락
- `401 Unauthorized`: 잘못된 토큰

---

## 🧪 테스트 API (개발 환경 전용)

> **⚠️ 보안 주의**: 이 API들은 `local` 프로필에서만 활성화됩니다.

#### 5. 사용자 정보 조회
```http
GET /api/auth/test/user
```

**Headers**:
```
Authorization: Bearer {ACCESS_TOKEN}
```

**권한**: `USER` 역할 필요

**Response**:
```json
{
  "userId": 123,
  "provider": "google",
  "message": "USER 권한으로 접근 성공"
}
```

---

#### 6. 관리자 정보 조회
```http
GET /api/auth/test/admin
```

**권한**: `ADMIN` 역할 필요 (현재 접근 불가)

**Response**:
```json
{
  "message": "ADMIN 권한으로 접근 성공"
}
```

---

#### 7. 인증 상태 확인
```http
GET /api/auth/test/authenticated
```

**Headers**:
```
Authorization: Bearer {ACCESS_TOKEN}
```

**권한**: 인증된 사용자

**Response**:
```json
{
  "authenticated": true,
  "userId": 123,
  "message": "인증된 사용자 접근 성공"
}
```

---

#### 8. 사용자 역할 변경 (테스트용)
```http
POST /api/auth/test/change-role/{role}
```

**Headers**:
```
Authorization: Bearer {ACCESS_TOKEN}
```

**권한**: `USER` 역할 필요

**Path Parameters**:
- `role`: 변경할 역할 (현재는 `USER`만 허용)

**Response**:
```json
{
  "message": "역할이 USER로 변경되었습니다",
  "newRole": "USER",
  "note": "새 토큰 발급을 위해 다시 로그인해주세요"
}
```

---

## 🏥 시스템 모니터링

#### 9. 헬스체크
```http
GET /actuator/health
```

**인증**: 불필요

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

---

## 🔒 보안 정책

### JWT 토큰
- **액세스 토큰**: 30분 유효
- **리프레시 토큰**: 7일 유효
- **알고리즘**: HMAC256

### 인증 헤더 형식
```
Authorization: Bearer {JWT_TOKEN}
```

### 지원 OAuth Provider
1. **Google**: `com.example.app://oauth/google/callback`
2. **Kakao**: `com.example.app://oauth/kakao/callback`
3. **Naver**: `com.example.app://oauth/naver/callback`

---

## 🚨 에러 응답 형식

```json
{
  "message": "에러 메시지",
  "code": "E001",
  "status": 400,
  "timestamp": "2024-01-01T12:00:00",
  "errors": [
    {
      "field": "code",
      "value": "",
      "reason": "인증 코드는 필수입니다"
    }
  ]
}
```

### 주요 에러 코드
- `E001`: 잘못된 입력값
- `E002`: 잘못된 타입
- `E003`: 필수 요청 파라미터 누락
- `E101`: 인증 필요
- `E102`: 유효하지 않은 토큰
- `E103`: 만료된 토큰
- `E201`: 접근 거부
- `E251`: 지원하지 않는 HTTP 메서드
- `E301`: 요청한 리소스를 찾을 수 없음
- `E302`: 사용자를 찾을 수 없음
- `E401`: 이미 존재하는 리소스
- `E501`: 서버 내부 오류
- `E601`: 지원하지 않는 OAuth 제공자
- `E602`: OAuth 토큰 요청 실패
- `E603`: OAuth 사용자 정보 요청 실패

---

## 🌍 CORS 정책

Flutter 앱과의 통신을 위해 CORS가 설정되어 있습니다.

**허용된 Origins**: `http://localhost:*` (개발 환경)  
**허용된 Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`  
**허용된 Headers**: `Authorization`, `Content-Type`  

---

## 📊 환경별 설정

### Local 환경 (`application-local.properties`)
- 상세한 디버그 로깅
- 모든 Actuator 엔드포인트 노출
- 테스트 컨트롤러 활성화
- SpringDoc OpenAPI 활성화

### Production 환경 (`application-production.properties`)
- 최소한의 로깅 (WARN 레벨)
- 보안 강화 설정
- API 문서 비활성화
- 테스트 엔드포인트 비활성화

---

## 📝 API 사용 예시

### 1. Google OAuth 로그인 플로우

```bash
# 1. OAuth 로그인
curl -X POST http://localhost:8080/api/v1/oauth/login/google \
  -H "Content-Type: application/json" \
  -d '{"code": "google_authorization_code"}'

# 응답
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "isNewUser": false
}
```

### 2. 인증이 필요한 API 호출

```bash
# 사용자 정보 조회
curl -X GET http://localhost:8080/api/auth/test/user \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 응답
{
  "userId": 123,
  "provider": "google",
  "message": "USER 권한으로 접근 성공"
}
```

### 3. 토큰 갱신

```bash
# 리프레시 토큰으로 새 액세스 토큰 발급
curl -X POST http://localhost:8080/auth/refresh \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 응답
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## 🔧 개발자 가이드

### 프로젝트 실행

```bash
# 로컬 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 프로덕션 환경에서 실행
./gradlew bootRun --args='--spring.profiles.active=production'
```

### 환경 변수 설정

`.env` 파일을 생성하고 다음 환경 변수를 설정하세요:

```bash
# JWT 시크릿 키
JWT_SECRET_KEY=your_jwt_secret_key

# 데이터베이스 설정
DB_URL=jdbc:postgresql://localhost:5432/neulbo
DB_USERNAME=postgres
DB_PASSWORD=password

# Redis 설정
REDIS_HOST=localhost

# OAuth 클라이언트 설정
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret
```

---

**작성일**: 2024년 12월  
**작성자**: Neulbo Backend Team  
**버전**: 1.0.0  

> 이 API 명세서는 현재 구현된 기능을 바탕으로 작성되었으며, 향후 추가 기능 개발 시 업데이트될 예정입니다. 