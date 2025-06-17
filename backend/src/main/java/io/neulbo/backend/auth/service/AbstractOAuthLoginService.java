package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.LoginResponse;
import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOAuthLoginService implements OAuthLoginService {

    protected final WebClient webClient;
    protected final UserRepository userRepository;
    protected final JwtProvider jwtProvider;

    /**
     * 블로킹 로그인 로직 (하위 호환성을 위해)
     * 
     * 이 메서드는 리액티브 체인 외부에서만 사용되어야 합니다.
     * 리액티브 컨텍스트에서는 loginReactive()를 사용하세요.
     * 
     * 1. 토큰 획득
     * 2. 사용자 정보 조회
     * 3. 사용자 조회/생성 (동시성 안전)
     * 4. JWT 토큰 생성 및 반환
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoginResponse login(String code, String provider) {
        // 별도 스레드에서 리액티브 메서드를 블로킹 호출로 변환
        return loginReactive(code, provider)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    /**
     * 리액티브 공통 로그인 로직
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Mono<LoginResponse> loginReactive(String code, String provider) {
        return getTokenReactive(code)
                .flatMap(token -> getUserInfoReactive(token.accessToken())
                        .flatMap(userInfo -> findOrCreateUserReactive(userInfo, provider.toLowerCase())
                                .map(result -> {
                                    List<String> roles = createSafeRolesList(result.user().getRole());
                                    
                                    return new LoginResponse(
                                            jwtProvider.createAccessToken(result.user().getId(), roles),
                                            jwtProvider.createRefreshToken(result.user().getId()),
                                            result.isNewUser()
                                    );
                                })))
                .subscribeOn(Schedulers.boundedElastic()); // 데이터베이스 작업을 위한 스케줄러
    }

    /**
     * 사용자 생성 결과를 담는 레코드
     */
    private record UserCreationResult(User user, boolean isNewUser) {}

    /**
     * 안전한 역할 리스트 생성
     * 
     * null이나 빈 문자열인 경우 기본 "USER" 역할을 제공하여
     * List.of(null) NullPointerException을 방지합니다.
     * 
     * @param role 사용자 역할 (null 가능)
     * @return 안전한 역할 리스트 (항상 최소 하나의 역할 포함)
     */
    private List<String> createSafeRolesList(String role) {
        if (role != null && !role.trim().isEmpty()) {
            return List.of(role.trim());
        }
        
        log.debug("User role is null or empty, assigning default USER role");
        return List.of("USER");
    }

    /**
     * 사용자 조회 또는 생성 (동시성 안전)
     * UNIQUE 제약조건과 예외 처리를 통해 중복 생성 방지
     */
    private UserCreationResult findOrCreateUser(OAuthUser userInfo, String provider) {
        // 1차: 기존 사용자 조회
        Optional<User> existingUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider);
        if (existingUser.isPresent()) {
            return new UserCreationResult(existingUser.get(), false);
        }

        // 2차: 새 사용자 생성 시도
        try {
            User newUser = User.builder()
                    .provider(provider)
                    .socialId(userInfo.id())
                    .nickname(userInfo.nickname())
                    .email(userInfo.email())
                    .profileImage(userInfo.profileImage())
                    .build();
            
            User savedUser = userRepository.save(newUser);
            return new UserCreationResult(savedUser, true);
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 중복 생성 시도 시 발생
            log.warn("Duplicate user creation attempt for provider: {}, socialId: {}", provider, userInfo.id());
            
            // 3차: 다시 조회 (다른 스레드에서 생성된 사용자)
            Optional<User> retryUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider);
            if (retryUser.isPresent()) {
                return new UserCreationResult(retryUser.get(), false);
            }
            
            // 예상치 못한 상황
            log.error("Failed to find user after DataIntegrityViolationException for provider: {}, socialId: {}", provider, userInfo.id());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 리액티브 사용자 조회 또는 생성 (동시성 안전)
     * UNIQUE 제약조건과 예외 처리를 통해 중복 생성 방지
     */
    private Mono<UserCreationResult> findOrCreateUserReactive(OAuthUser userInfo, String provider) {
        return Mono.fromCallable(() -> {
            // 1차: 기존 사용자 조회
            Optional<User> existingUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider);
            if (existingUser.isPresent()) {
                return new UserCreationResult(existingUser.get(), false);
            }

            // 2차: 새 사용자 생성 시도
            try {
                User newUser = User.builder()
                        .provider(provider)
                        .socialId(userInfo.id())
                        .nickname(userInfo.nickname())
                        .email(userInfo.email())
                        .profileImage(userInfo.profileImage())
                        .build();
                
                User savedUser = userRepository.save(newUser);
                return new UserCreationResult(savedUser, true);
            } catch (DataIntegrityViolationException e) {
                // 동시성으로 인한 중복 생성 시도 시 발생
                log.warn("Duplicate user creation attempt for provider: {}, socialId: {}", provider, userInfo.id());
                
                // 3차: 다시 조회 (다른 스레드에서 생성된 사용자)
                Optional<User> retryUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider);
                if (retryUser.isPresent()) {
                    return new UserCreationResult(retryUser.get(), false);
                }
                
                // 예상치 못한 상황
                log.error("Failed to find user after DataIntegrityViolationException for provider: {}, socialId: {}", provider, userInfo.id());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // 데이터베이스 작업을 별도 스레드에서 실행
        .onErrorMap(Exception.class, e -> {
            if (e instanceof BusinessException) {
                return e;
            }
            log.error("Failed to find or create user for provider: {}, socialId: {}", provider, userInfo.id(), e);
            return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        });
    }

    // 각 구현체에서 제공자별 로직을 구현해야 하는 추상 메서드들
    @Override
    public abstract OAuthToken getToken(String code);

    @Override
    public abstract OAuthUser getUserInfo(String accessToken);

    @Override
    public abstract Mono<OAuthToken> getTokenReactive(String code);

    @Override
    public abstract Mono<OAuthUser> getUserInfoReactive(String accessToken);
} 