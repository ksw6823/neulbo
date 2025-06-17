package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
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

    private final UserRepository userRepository;

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

        // 보안 강화: ADMIN 역할 할당 차단
        if ("ADMIN".equalsIgnoreCase(role)) {
            log.warn("ADMIN role assignment blocked for user ID: {}", userId);
            return ResponseEntity.status(403).body(Map.of(
                    "error", "보안상의 이유로 ADMIN 역할은 할당할 수 없습니다",
                    "reason", "권한 상승 공격 방지"
            ));
        }

        // 허용된 역할만 설정 가능 (USER만 허용)
        if (!"USER".equalsIgnoreCase(role)) {
            log.warn("Invalid role assignment attempt: {} for user ID: {}", role, userId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "허용되지 않은 역할입니다. USER만 가능합니다",
                    "allowedRoles", "USER"
            ));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found for role change - User ID: {}", userId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "사용자를 찾을 수 없습니다"
            ));
        }

        // 현재 역할과 동일한 경우 변경하지 않음
        if ("USER".equals(user.getRole())) {
            log.info("Role change skipped - User {} already has USER role", userId);
            return ResponseEntity.ok(Map.of(
                    "message", "이미 USER 역할입니다",
                    "currentRole", "USER",
                    "note", "변경이 필요하지 않습니다"
            ));
        }

        // 역할 변경 (USER로만 제한)
        userRepository.updateUserRole(userId, "USER");
        log.info("Role successfully changed to USER for user ID: {}", userId);

        return ResponseEntity.ok(Map.of(
                "message", "역할이 USER로 변경되었습니다",
                "newRole", "USER",
                "note", "새 토큰 발급을 위해 다시 로그인해주세요"
        ));
    }
} 