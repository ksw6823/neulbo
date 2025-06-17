package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * JWT 인증 및 권한 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
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
     */
    @PostMapping("/change-role/{role}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> changeUserRole(@PathVariable String role) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "사용자 ID를 찾을 수 없습니다"
            ));
        }

        // 허용된 역할만 설정 가능
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "허용되지 않은 역할입니다. USER 또는 ADMIN만 가능합니다"
            ));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "사용자를 찾을 수 없습니다"
            ));
        }

        // 역할 변경 (실제로는 User 엔티티에 setter가 필요하거나 Builder 패턴 사용)
        // 현재는 테스트용이므로 직접 업데이트 쿼리 사용
        userRepository.updateUserRole(userId, role);

        return ResponseEntity.ok(Map.of(
                "message", "역할이 " + role + "로 변경되었습니다",
                "newRole", role,
                "note", "새 토큰 발급을 위해 다시 로그인해주세요"
        ));
    }
} 