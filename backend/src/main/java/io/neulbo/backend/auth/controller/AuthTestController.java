package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWT 인증 및 권한 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
public class AuthTestController {

    /**
     * 인증된 사용자만 접근 가능한 엔드포인트
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
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
        Long userId = SecurityUtils.getCurrentUserId();
        
        return ResponseEntity.ok(Map.of(
                "authenticated", isAuthenticated,
                "userId", userId,
                "message", "인증된 사용자 접근 성공"
        ));
    }
} 