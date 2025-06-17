# 보안 가이드라인

## AuthTestController 보안 개선사항

### 🚨 발견된 보안 취약점
AuthTestController의 `changeUserRole` 엔드포인트에서 심각한 권한 상승 공격 취약점이 발견되었습니다.

**취약점 상세:**
- USER 권한을 가진 모든 사용자가 자신을 ADMIN으로 승격 가능
- 인증된 사용자라면 누구나 최고 권한 획득 가능
- 프로덕션 환경에서도 활성화되어 실제 보안 위험 존재

### ✅ 적용된 보안 개선사항

#### 1. 환경별 활성화 제한
```java
@Profile("local") // 개발 환경에서만 활성화
@RestController
public class AuthTestController {
    // 프로덕션 환경에서는 자동으로 비활성화
}
```

#### 2. ADMIN 역할 할당 차단
```java
// 보안 강화: ADMIN 역할 할당 차단
if ("ADMIN".equalsIgnoreCase(role)) {
    log.warn("ADMIN role assignment blocked for user ID: {}", userId);
    return ResponseEntity.status(403).body(Map.of(
        "error", "보안상의 이유로 ADMIN 역할은 할당할 수 없습니다",
        "reason", "권한 상승 공격 방지"
    ));
}
```

#### 3. 포괄적인 보안 로깅
- 모든 역할 변경 시도 기록
- 실패한 권한 상승 시도 경고 로그
- 사용자 ID, 제공자, 요청된 역할 추적

#### 4. 허용 역할 제한
- USER 역할로만 변경 가능
- 다른 모든 역할 요청 차단
- 명확한 오류 메시지 제공

### 🛡️ 보안 검증 테스트

#### 프로파일별 활성화 테스트
- `local` 프로파일: 컨트롤러 활성화 ✅
- `production` 프로파일: 컨트롤러 비활성화 ✅
- 기본 프로파일: 컨트롤러 비활성화 ✅

### 📋 권장사항

#### 프로덕션 배포 시
1. **환경 변수 확인**: `SPRING_PROFILES_ACTIVE`가 `production`으로 설정되어 있는지 확인
2. **엔드포인트 접근 테스트**: `/api/auth/test/**` 경로가 404를 반환하는지 확인
3. **로그 모니터링**: 권한 변경 시도 로그를 주기적으로 확인

#### 개발 환경에서
1. **테스트 목적으로만 사용**: 실제 사용자 데이터로 테스트하지 않기
2. **로그 확인**: 모든 역할 변경 시도가 적절히 로깅되는지 확인
3. **권한 테스트**: 다양한 권한 시나리오 테스트

### 🔍 모니터링 지표

#### 보안 이벤트 로그
```
WARN  - Role change attempt - User ID: {}, Provider: {}, Requested Role: {}
WARN  - ADMIN role assignment blocked for user ID: {}
ERROR - Role change failed - User ID not found
```

#### 알림 설정 권장사항
- ADMIN 역할 할당 시도 시 즉시 알림
- 비정상적인 역할 변경 패턴 감지
- 프로덕션 환경에서 테스트 엔드포인트 접근 시도 감지

### 📚 관련 파일
- `AuthTestController.java` - 보안 개선된 컨트롤러
- `AuthTestControllerSecurityTest.java` - 보안 검증 테스트
- `SECURITY.md` - 이 보안 가이드라인

### 📋 SpringDoc OpenAPI 보안 설정

#### 프로덕션 환경에서 API 문서 완전 차단
```properties
# application-production.properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
springdoc.paths-to-exclude=/api/auth/test/**,/api/test/**,/test/**
```

#### 개발 환경에서만 API 문서 제공
```properties
# application-local.properties
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

### 🔧 환경별 설정 파일

#### `application-production.properties`
- ✅ 에러 스택트레이스 비활성화
- ✅ 상세 로깅 비활성화
- ✅ API 문서 완전 차단
- ✅ Actuator 엔드포인트 최소화

#### `application-local.properties`
- ✅ 상세 디버그 로깅 활성화
- ✅ API 문서 제공
- ✅ 모든 Actuator 엔드포인트 노출
- ✅ 상세 에러 정보 표시

### 🚀 향후 개선 계획
1. **역할 기반 접근 제어 강화**: 더 세분화된 권한 체계 구현
2. **감사 로그 시스템**: 모든 권한 변경 이력을 데이터베이스에 저장
3. **2단계 인증**: 중요한 권한 변경 시 추가 인증 요구
4. **자동 보안 스캔**: CI/CD 파이프라인에 보안 취약점 검사 통합
5. **API 게이트웨이**: 추가적인 보안 레이어 구현 