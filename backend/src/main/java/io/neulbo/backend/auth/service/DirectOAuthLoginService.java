package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.LoginResponse;
import io.neulbo.backend.auth.dto.OAuthUserDataRequest;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

/**
 * 사용자 데이터 기반 OAuth 인증 서비스
 * 프론트엔드에서 직접 받아온 OAuth 사용자 정보로 로그인/회원가입 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectOAuthLoginService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 사용자 데이터 기반 로그인/회원가입 (리액티브)
     * 
     * @param userData 프론트엔드에서 받아온 OAuth 사용자 데이터 (provider 정보 포함)
     * @return JWT 토큰을 포함한 로그인 응답
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Mono<LoginResponse> loginWithUserData(OAuthUserDataRequest userData) {
        return Mono.fromCallable(() -> processUserLogin(userData))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 동기 방식 로그인 처리 (하위 호환성)
     * 
     * @param userData OAuth 사용자 데이터 (provider 정보 포함)
     * @return JWT 토큰을 포함한 로그인 응답
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public LoginResponse loginWithUserDataBlocking(OAuthUserDataRequest userData) {
        return processUserLogin(userData);
    }

    /**
     * 실제 로그인 처리 로직
     */
    private LoginResponse processUserLogin(OAuthUserDataRequest userData) {
        // 입력 데이터 검증
        validateUserData(userData);

        // 사용자 조회 또는 생성
        UserCreationResult result = findOrCreateUser(userData);
        
        log.info("OAuth 로그인 완료 - Provider: {}, User ID: {}, 신규 사용자: {}", 
                userData.getNormalizedProvider(), result.user().getId(), result.isNewUser());

        // JWT 토큰 생성 및 응답
        return new LoginResponse(
                jwtProvider.createAccessToken(result.user().getId()),
                jwtProvider.createRefreshToken(result.user().getId()),
                result.isNewUser()
        );
    }

    /**
     * 입력 데이터 유효성 검증
     */
    private void validateUserData(OAuthUserDataRequest userData) {
        if (userData == null) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_USER_DATA, "사용자 데이터가 null입니다");
        }
        
        if (!userData.hasRequiredFields()) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_USER_DATA, "필수 필드(provider, providerId)가 누락되었습니다");
        }
        
        if (!userData.isSupportedProvider()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER, 
                    "지원되지 않는 OAuth 제공자입니다: " + userData.provider());
        }

        // 제공자별 추가 검증
        validateProviderSpecificData(userData);
    }

    /**
     * 제공자별 특별한 검증 로직
     */
    private void validateProviderSpecificData(OAuthUserDataRequest userData) {
        String provider = userData.getNormalizedProvider();
        
        switch (provider) {
            case "google":
                // Google은 이메일이 필수
                if (!userData.hasEmail()) {
                    throw new BusinessException(ErrorCode.INVALID_OAUTH_USER_DATA, 
                            "Google 로그인은 이메일이 필수입니다");
                }
                break;
            case "kakao":
                // Kakao는 providerId만 있으면 됨 (이메일 선택적)
                break;
            case "naver":
                // Naver도 이메일 권장하지만 필수는 아님
                break;
            default:
                log.warn("알려지지 않은 OAuth 제공자: {}", provider);
        }
    }

    /**
     * 사용자 조회 또는 생성
     */
    private UserCreationResult findOrCreateUser(OAuthUserDataRequest userData) {
        String normalizedProvider = userData.getNormalizedProvider();
        
        // 1. 기존 사용자 조회 (provider + providerId 조합)
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(
                normalizedProvider, userData.providerId());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // 기존 사용자 정보 업데이트 (선택적)
            updateUserIfNeeded(user, userData);
            
            log.debug("기존 사용자 로그인 - Provider: {}, Provider ID: {}, User ID: {}", 
                    normalizedProvider, userData.providerId(), user.getId());
            
            return new UserCreationResult(user, false);
        }

        // 2. 이메일로 기존 계정 확인 (다른 제공자로 가입한 경우)
        if (userData.hasEmail()) {
            Optional<User> emailUser = userRepository.findByEmail(userData.email());
            if (emailUser.isPresent()) {
                // 이미 다른 제공자로 가입된 이메일
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, 
                        String.format("이메일 %s은 이미 다른 방법으로 가입된 계정입니다", userData.email()));
            }
        }

        // 3. 새 사용자 생성
        return createNewUser(userData);
    }

    /**
     * 기존 사용자 정보 업데이트 (프로필 정보가 변경된 경우)
     */
    private void updateUserIfNeeded(User user, OAuthUserDataRequest userData) {
        boolean needsUpdate = false;
        
        // 이메일 업데이트 (기존에 없었던 경우)
        if (userData.hasEmail() && !user.hasEmail()) {
            user.updateEmail(userData.email());
            needsUpdate = true;
        }
        
        // 이름 업데이트 (더 완전한 정보로)
        if (userData.hasName() && (user.getFullName() == null || user.getFullName().isEmpty())) {
            // User 엔티티에 setFullName 메서드가 있다고 가정
            // user.setFullName(userData.name());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            userRepository.save(user);
            log.debug("사용자 정보 업데이트됨 - User ID: {}", user.getId());
        }
    }

    /**
     * 새 사용자 생성
     */
    private UserCreationResult createNewUser(OAuthUserDataRequest userData) {
        String provider = userData.getNormalizedProvider();
        
        try {
            User newUser = User.createOAuthUser(
                    provider,
                    userData.providerId(),
                    userData.email(),
                    userData.getDisplayName(), // 닉네임 대신 표시명 사용
                    userData.name()
            );
            
            User savedUser = userRepository.save(newUser);
            
            log.info("새 OAuth 사용자 생성 완료 - Provider: {}, Provider ID: {}, User ID: {}", 
                    provider, userData.providerId(), savedUser.getId());
            
            return new UserCreationResult(savedUser, true);
            
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 인한 중복 생성 시도 - 재시도
            log.warn("사용자 생성 중 중복 발생, 재조회 시도 - Provider: {}, Provider ID: {}", 
                    provider, userData.providerId());
            
            Optional<User> user = userRepository.findByProviderAndProviderId(provider, userData.providerId());
            if (user.isPresent()) {
                return new UserCreationResult(user.get(), false);
            }
            
            throw new BusinessException(ErrorCode.USER_CREATION_FAILED, 
                    "사용자 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 사용자 생성 결과를 담는 레코드
     */
    private record UserCreationResult(User user, boolean isNewUser) {}
}