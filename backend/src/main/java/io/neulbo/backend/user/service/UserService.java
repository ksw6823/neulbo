package io.neulbo.backend.user.service;

import io.neulbo.backend.user.domain.User;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * 사용자 역할을 변경합니다.
     * 
     * @param userId 변경할 사용자 ID
     * @param newRole 새로운 역할
     * @return 역할 변경 성공 여부
     * @throws IllegalArgumentException 유효하지 않은 역할이거나 사용자가 존재하지 않는 경우
     * @throws SecurityException 보안상 허용되지 않는 역할 변경인 경우
     */
    boolean changeUserRole(Long userId, String newRole);
    
    /**
     * 사용자 ID로 사용자를 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    User findUserById(Long userId);
} 