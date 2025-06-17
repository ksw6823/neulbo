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

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOAuthLoginService implements OAuthLoginService {

    protected final WebClient webClient;
    protected final UserRepository userRepository;
    protected final JwtProvider jwtProvider;

    /**
     * 공통 로그인 로직
     * 1. 토큰 획득
     * 2. 사용자 정보 조회
     * 3. 사용자 조회/생성 (동시성 안전)
     * 4. JWT 토큰 생성 및 반환
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoginResponse login(String code, String provider) {
        OAuthToken token = getToken(code);
        OAuthUser userInfo = getUserInfo(token.accessToken());

        UserCreationResult result = findOrCreateUser(userInfo, provider.toLowerCase());

        return new LoginResponse(
                jwtProvider.createAccessToken(result.user().getId()),
                jwtProvider.createRefreshToken(result.user().getId()),
                result.isNewUser()
        );
    }

    /**
     * 사용자 생성 결과를 담는 레코드
     */
    private record UserCreationResult(User user, boolean isNewUser) {}

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

    // 각 구현체에서 제공자별 로직을 구현해야 하는 추상 메서드들
    @Override
    public abstract OAuthToken getToken(String code);

    @Override
    public abstract OAuthUser getUserInfo(String accessToken);
} 