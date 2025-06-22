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

    /**
     * 사용자 역할 변경 엔드포인트 (테스트용)
     * 
     * ⚠️ 보안 경고: 이 엔드포인트는 권한 상승 공격을 방지하기 위해 제한적으로 구현되었습니다.
     * - ADMIN 역할 할당은 차단됩니다
     * - 개발 환경에서만 사용 가능합니다
     * - 모든 역할 변경 시도가 로깅됩니다
     */
    @PostMapping("/change-role/{role}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> changeUserRole(@PathVariable String role) {
        Long userId = SecurityUtils.getCurrentUserId();
        String currentProvider = SecurityUtils.getCurrentUserProvider();
        
        // 보안 로깅: 모든 역할 변경 시도를 기록
        log.warn("Role change attempt - User ID: {}, Provider: {}, Requested Role: {}", 
                userId, currentProvider, role);
        
        if (userId == null) {
            log.error("Role change failed - User ID not found");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "사용자 ID를 찾을 수 없습니다"
            ));
        }

        try {
            // 서비스 계층을 통해 역할 변경 처리 (모든 검증 로직은 서비스에서 처리)
            boolean roleChanged = userService.changeUserRole(userId, role);
            
            if (!roleChanged) {
                // 이미 동일한 역할인 경우
                return ResponseEntity.ok(Map.of(
                        "message", "이미 USER 역할입니다",
                        "currentRole", "USER",
                        "note", "변경이 필요하지 않습니다"
                ));
            }
            
            log.info("Role successfully changed to {} for user ID: {}", role, userId);
        } catch (IllegalArgumentException e) {
            log.error("Role change failed - Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (SecurityException e) {
            log.warn("Role change blocked for security reasons: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of(
                    "error", e.getMessage(),
                    "reason", "보안 정책 위반"
            ));
        } catch (Exception e) {
            log.error("Role change failed with unexpected error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "역할 변경 처리 중 오류가 발생했습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "역할이 " + role.toUpperCase() + "로 변경되었습니다",
                "newRole", role.toUpperCase(),
                "note", "새 토큰 발급을 위해 다시 로그인해주세요"
        ));
    }
} 