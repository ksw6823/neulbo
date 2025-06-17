package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.dto.*;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.auth.service.OAuthLoginService;
import io.neulbo.backend.auth.service.RefreshTokenService;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthLoginController {

    private final Map<String, OAuthLoginService> loginServices;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * OAuth 로그인 (리액티브 방식 - 권장)
     */
    @PostMapping("/login/{provider}")
    public Mono<ResponseEntity<LoginResponse>> login(
            @PathVariable String provider,
            @RequestBody OAuthCodeRequest request) {
        OAuthLoginService service = loginServices.get(provider.toLowerCase());
        if (service == null) {
            return Mono.error(new BusinessException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
        }

        return service.loginReactive(request.code(), provider)
                .map(ResponseEntity::ok)
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof BusinessException) {
                        return e;
                    }
                    return new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED, e);
                });
    }

    /**
     * OAuth 로그인 (블로킹 방식 - 하위 호환성)
     * 
     * @deprecated 리액티브 방식 사용을 권장합니다.
     */
    @PostMapping("/login/{provider}/blocking")
    public ResponseEntity<LoginResponse> loginBlocking(
            @PathVariable String provider,
            @RequestBody OAuthCodeRequest request) {
        OAuthLoginService service = loginServices.get(provider.toLowerCase());
        if (service == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }

        try {
            // 블로킹 메서드는 리액티브 체인 외부에서만 사용
            LoginResponse response = service.login(request.code(), provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED, e);
        }
    }
}
