package io.neulbo.backend.user.service;

import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    // 허용되는 역할 목록 (보안상 제한적으로 설정)
    private static final Set<String> ALLOWED_ROLES = Set.of("USER");
    
    // 보안상 금지된 역할 목록
    private static final Set<String> FORBIDDEN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    
    @Override
    @Transactional
    public boolean changeUserRole(Long userId, String newRole) {
        // 입력 검증
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다");
        }
        
        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 null이거나 비어있을 수 없습니다");
        }
        
        String normalizedRole = newRole.trim().toUpperCase();
        
        // 사용자 존재 검증 (역할 검증 전에 수행)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("역할 변경 실패 - 사용자 찾을 수 없음: User ID {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
                });
        
        // 현재 역할과 동일한 경우 변경하지 않음 (보안 검증 전에 수행)
        // 대소문자를 무시하고 비교하여 "user", "User", "USER" 등을 모두 처리
        if (normalizedRole.equalsIgnoreCase(user.getRole())) {
            log.info("역할 변경 건너뜀 - 사용자 {}는 이미 {} 역할", userId, normalizedRole);
            return false; // 변경되지 않음
        }
        
        // 보안 검증: 금지된 역할 차단
        if (FORBIDDEN_ROLES.contains(normalizedRole)) {
            log.warn("금지된 역할 할당 시도 차단 - User ID: {}, Role: {}", userId, normalizedRole);
            throw new SecurityException("보안상의 이유로 " + normalizedRole + " 역할은 할당할 수 없습니다");
        }
        
        // 허용된 역할 검증
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            log.warn("허용되지 않은 역할 할당 시도 - User ID: {}, Role: {}", userId, normalizedRole);
            throw new IllegalArgumentException("허용되지 않은 역할입니다. 허용된 역할: " + ALLOWED_ROLES);
        }
        
        // 역할 변경 실행
        int updatedRows = userRepository.updateUserRole(userId, normalizedRole);
        
        if (updatedRows > 0) {
            log.info("역할 변경 성공 - User ID: {}, Old Role: {}, New Role: {}", 
                    userId, user.getRole(), normalizedRole);
            return true;
        } else {
            log.error("역할 변경 실패 - 데이터베이스 업데이트 실패: User ID {}", userId);
            throw new RuntimeException("역할 변경에 실패했습니다");
        }
    }
    
    @Override
    public User findUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다");
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 조회 실패 - User ID: {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
                });
    }
} 