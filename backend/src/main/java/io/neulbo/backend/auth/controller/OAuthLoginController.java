package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.dto.*;
import io.neulbo.backend.auth.service.DirectOAuthLoginService;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/oauth") // 자동으로 /api/v1/oauth가 됩니다
@RequiredArgsConstructor
public class OAuthLoginController {

    private final DirectOAuthLoginService directOAuthLoginService;

    /**
     * OAuth 로그인 (리액티브 방식 - 권장)
     * 프론트엔드에서 OAuth 제공자로부터 직접 받아온 사용자 정보로 로그인/회원가입 처리
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @Valid @RequestBody OAuthUserDataRequest request) {
        
        return directOAuthLoginService.loginWithUserData(request)
                .map(ResponseEntity::ok)
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof BusinessException) {
                        return e;
                    }
                    return new BusinessException(ErrorCode.OAUTH_LOGIN_FAILED, 
                            "OAuth 로그인 중 오류가 발생했습니다", e);
                });
    }

    /**
     * OAuth 로그인 (블로킹 방식 - 하위 호환성)
     */
    @PostMapping("/login/blocking")
    public ResponseEntity<LoginResponse> loginBlocking(
            @Valid @RequestBody OAuthUserDataRequest request) {
        
        try {
            LoginResponse response = directOAuthLoginService.loginWithUserDataBlocking(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ErrorCode.OAUTH_LOGIN_FAILED, 
                    "OAuth 로그인 중 오류가 발생했습니다", e);
        }
    }

}
