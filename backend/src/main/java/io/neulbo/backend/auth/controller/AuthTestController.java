package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * JWT 인증 및 권한 테스트용 컨트롤러
 * 
 * ⚠️ 보안 경고: 이 컨트롤러는 테스트 목적으로만 사용되며 개발 환경에서만 활성화됩니다.
 * 프로덕션 환경에서는 자동으로 비활성화되어 보안 취약점을 방지합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
@Profile("local") // 개발 환경(local)에서만 활성화
public class AuthTestController {

    private final UserService userService;

    /**
     * 인증된 사용자만 접근 가능한 엔드포인트
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        UUID userId = SecurityUtils.getCurrentUserId();
        String provider = SecurityUtils.getCurrentUserProvider();
        
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "provider", provider,
                "message", "USER 권한으로 접근 성공"
        ));
    }

    /**
     * 관리자만 접근 가능한 엔드포인트 (현재는 접근 불가)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getAdminInfo() {
        return ResponseEntity.ok(Map.of(
                "message", "ADMIN 권한으로 접근 성공"
        ));
    }

    /**
     * 인증 여부만 확인하는 엔드포인트
     */
    @GetMapping("/authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuthenticatedInfo() {
        boolean isAuthenticated = SecurityUtils.isAuthenticated();
        UUID userId = SecurityUtils.getCurrentUserId();
        
        return ResponseEntity.ok(Map.of(
                "authenticated", isAuthenticated,
                "userId", userId,
                "message", "인증된 사용자 접근 성공"
        ));
    }
} 