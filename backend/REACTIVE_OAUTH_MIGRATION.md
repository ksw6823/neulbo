# OAuth 서비스 리액티브 마이그레이션 가이드

## 개요

이 문서는 Spring Boot OAuth 인증 시스템을 동기 방식에서 완전한 리액티브 방식으로 마이그레이션한 내용을 설명합니다.

## 마이그레이션 목표

### 🎯 주요 목표
- **Netty 이벤트 루프 보호**: WebClient `block()` 호출로 인한 이벤트 루프 블로킹 제거
- **성능 향상**: 리액티브 스트림을 통한 비동기 처리로 동시성 및 처리량 증대
- **확장성 개선**: 높은 동시 요청 처리 능력 확보
- **리소스 효율성**: 메모리 사용량 최적화 및 백프레셔 자동 처리

### ⚠️ 해결된 문제
- WebClient `block()` 호출로 인한 데드락 위험
- OAuth Provider 장애 시 무한 대기 문제
- 높은 동시성 환경에서의 성능 저하
- 예외 스택 트레이스 손실 문제

## 아키텍처 변경사항

### Before (동기 방식)
```java
public LoginResponse login(String code, String provider) {
    OAuthToken token = getToken(code);                    // block() 호출
    OAuthUser userInfo = getUserInfo(token.accessToken()); // block() 호출
    UserCreationResult result = findOrCreateUser(userInfo, provider);
    return createLoginResponse(result);
}
```

### After (리액티브 방식)
```java
public Mono<LoginResponse> loginReactive(String code, String provider) {
    return getTokenReactive(code)
            .flatMap(token -> getUserInfoReactive(token.accessToken())
                    .flatMap(userInfo -> findOrCreateUserReactive(userInfo, provider)
                            .map(this::createLoginResponse)))
            .subscribeOn(Schedulers.boundedElastic());
}
```

## 구현 상세

### 1. 리액티브 메서드 구현

#### 🔄 완전한 리액티브 체인
```java
@Override
public Mono<OAuthToken> getTokenReactive(String code) {
    return webClient.post()
            .uri(tokenUri)
            .body(BodyInserters.fromFormData(...))
            .retrieve()
            .onStatus(status -> status.isError(), this::handleError)
            .bodyToMono(Map.class)
            .map(this::mapToOAuthToken)
            .onErrorMap(Exception.class, this::wrapException);
}
```

#### 🛡️ 강화된 에러 처리
```java
.onStatus(
    status -> status.is4xxClientError() || status.is5xxServerError(),
    response -> {
        int statusCode = response.statusCode().value();
        return response.bodyToMono(String.class)
                .doOnNext(body -> log.error("Request failed - Status: {}, Response: {}", 
                        statusCode, truncateBody(body)))
                .map(body -> new BusinessException(
                        String.format("Request failed with status %d: %s", 
                                statusCode, truncateBody(body, 100)),
                        ErrorCode.OAUTH_TOKEN_REQUEST_FAILED));
    }
)
```

### 2. 하위 호환성 유지

기존 동기 API는 리액티브 메서드를 래핑하여 호환성을 유지합니다:

```java
@Override
public OAuthToken getToken(String code) {
    // 리액티브 메서드를 호출하고 블로킹으로 변환 (하위 호환성을 위해)
    return getTokenReactive(code)
            .subscribeOn(Schedulers.boundedElastic())
            .block();
}
```

### 3. 스케줄러 최적화

- **boundedElastic()**: 데이터베이스 작업 및 블로킹 fallback 용도
- **WebClient**: 이벤트 루프에서 직접 실행 (non-blocking)

### 4. WebClient 최적화

```java
@Bean
public WebClient webClient() {
    ConnectionProvider connectionProvider = ConnectionProvider.builder("oauth-pool")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .build();
            
    HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10));
            
    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(1024 * 1024))
            .build();
}
```

## 마이그레이션 단계별 진행

### Phase 1: KakaoLoginService 리액티브화 ✅
- 완전한 리액티브 메서드 구현
- `block()` 호출 완전 제거
- 헬퍼 메서드 및 에러 처리 개선

### Phase 2: GoogleLoginService & NaverLoginService 리액티브화 ✅
- KakaoLoginService와 동일한 패턴 적용
- 각 OAuth Provider별 응답 구조에 맞춘 매핑 로직
- 일관된 에러 처리 및 로깅

### Phase 3: AbstractOAuthLoginService 리액티브화 ✅
- 공통 로그인 로직 완전 리액티브화
- 데이터베이스 작업 리액티브 래핑
- 동시성 안전성 유지

### Phase 4: 최적화 및 정리 ✅
- WebClient 연결 풀 및 타임아웃 최적화
- 불필요한 타임아웃 설정 제거 (WebClient 레벨에서 처리)
- import 정리 및 코드 최적화

## 성능 개선 효과

### 🚀 처리량 향상
- **동시 요청 처리**: 이벤트 루프 블로킹 제거로 높은 동시성 지원
- **메모리 효율성**: 리액티브 스트림의 백프레셔로 메모리 사용량 최적화
- **연결 풀 관리**: OAuth Provider별 연결 재사용으로 네트워크 오버헤드 감소

### 🛡️ 안정성 향상
- **타임아웃 보장**: WebClient 레벨에서 연결/응답 타임아웃 설정
- **에러 추적**: HTTP 상태 코드 및 응답 본문 로깅으로 디버깅 개선
- **예외 체인**: 원본 예외 보존으로 완전한 스택 트레이스 제공

### 📊 모니터링 개선
- **상세 로깅**: OAuth 요청/응답 상태 및 내용 로깅
- **안전한 로그**: 응답 본문 자르기로 로그 크기 제한
- **구조화된 에러**: 일관된 에러 메시지 형식

## 사용 가이드

### 🚀 리액티브 API 사용 (권장)

#### 컨트롤러에서 리액티브 방식
```java
@PostMapping("/oauth/login/{provider}")
public Mono<ResponseEntity<LoginResponse>> login(
        @PathVariable String provider,
        @RequestBody OAuthCodeRequest request) {
    return oauthService.loginReactive(request.code(), provider)
            .map(ResponseEntity::ok)
            .onErrorMap(Exception.class, e -> {
                if (e instanceof BusinessException) {
                    return e;
                }
                return new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED, e);
            });
}
```

#### 서비스에서 리액티브 체이닝
```java
@Service
public class UserService {
    
    @Autowired
    private OAuthLoginService oauthService;
    
    public Mono<UserProfile> createUserProfile(String code, String provider) {
        return oauthService.loginReactive(code, provider)
                .flatMap(loginResponse -> userProfileService.createProfile(loginResponse))
                .doOnSuccess(profile -> log.info("User profile created: {}", profile.getId()))
                .doOnError(error -> log.error("Profile creation failed", error));
    }
}
```

### ⚠️ 블로킹 API 사용 (하위 호환성)

#### 전용 블로킹 엔드포인트
```java
@PostMapping("/oauth/login/{provider}/blocking")
public ResponseEntity<LoginResponse> loginBlocking(
        @PathVariable String provider,
        @RequestBody OAuthCodeRequest request) {
    // 블로킹 메서드는 리액티브 체인 외부에서만 사용
    LoginResponse response = oauthService.login(request.code(), provider);
    return ResponseEntity.ok(response);
}
```

#### 레거시 서비스 통합
```java
@Service
public class LegacyUserService {
    
    @Autowired
    private OAuthLoginService oauthService;
    
    public UserProfile handleLegacyLogin(String code, String provider) {
        // 블로킹 호출은 별도 스레드에서 실행됨
        LoginResponse response = oauthService.login(code, provider);
        return convertToUserProfile(response);
    }
}
```

## 주의사항

### ⚠️ 리액티브 체인 내 블로킹 금지
```java
// ❌ 잘못된 사용: 리액티브 체인 내에서 블로킹
public Mono<String> badExample(String code) {
    return someReactiveMono
            .flatMap(data -> {
                // 이렇게 하면 안됩니다!
                OAuthToken token = oauthService.getToken(code); // block() 호출
                return processToken(token);
            });
}

// ✅ 올바른 사용: 완전한 리액티브 체인
public Mono<String> goodExample(String code) {
    return someReactiveMono
            .flatMap(data -> oauthService.getTokenReactive(code) // 리액티브 메서드 사용
                    .flatMap(this::processToken));
}
```

### 📍 블로킹 메서드 사용 원칙
- **리액티브 체인 외부에서만** 블로킹 메서드 사용
- **전용 블로킹 엔드포인트** 또는 **레거시 통합**에서만 사용
- **새로운 코드**에서는 리액티브 메서드 우선 사용

### 🔄 인터페이스 분리
- `OAuthLoginService`: 리액티브 메서드 + 블로킹 메서드 (하위 호환성)
- `BlockingOAuthLoginService`: 블로킹 전용 인터페이스 (명시적 분리)
- 새로운 코드에서는 리액티브 메서드만 사용 권장

### 📝 에러 처리
- `BusinessException`을 통한 일관된 에러 처리
- 원본 예외를 cause로 보존
- 적절한 HTTP 상태 코드 매핑
- `onErrorMap()`을 통한 리액티브 에러 변환

### 🔧 설정 관리
- WebClient 타임아웃은 전역 설정에서 관리
- 개별 메서드 레벨 타임아웃 설정 불필요
- 연결 풀 설정으로 성능 최적화
- `subscribeOn(Schedulers.boundedElastic())`은 블로킹 래퍼에서만 사용

## 결론

OAuth 인증 시스템의 완전한 리액티브 마이그레이션을 통해:

- ✅ **이벤트 루프 블로킹 문제 완전 해결**
- ✅ **높은 동시성 및 확장성 확보**
- ✅ **향상된 에러 처리 및 디버깅**
- ✅ **하위 호환성 완벽 유지**
- ✅ **성능 및 리소스 효율성 개선**

이제 시스템은 높은 부하 환경에서도 안정적으로 동작하며, 향후 완전한 리액티브 아키텍처로의 확장을 위한 견고한 기반을 제공합니다. 