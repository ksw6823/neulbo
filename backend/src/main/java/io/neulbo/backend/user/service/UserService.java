package io.neulbo.backend.user.service;

import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.dto.*;
import java.util.UUID;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * 사용자 ID로 사용자를 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    User findUserById(UUID userId);
    User findByUserName(String userName);

    /**
     * 사용자 프로필을 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 프로필 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    ProfileResponse getProfile(UUID userId);

    /**
     * 사용자 프로필을 수정합니다.
     * 
     * @param userId 수정할 사용자 ID
     * @param request 프로필 수정 요청 정보
     * @return 수정된 프로필 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);

    /**
     * 사용자 계정 정보를 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 계정 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    AccountResponse getAccount(UUID userId);

    /**
     * 사용자 계정 정보를 수정합니다.
     * 
     * @param userId 수정할 사용자 ID
     * @param request 계정 수정 요청 정보
     * @return 수정된 계정 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    AccountResponse updateAccount(UUID userId, AccountUpdateRequest request);

    /**
     * 사용자 계정을 삭제합니다.
     * 
     * @param userId 삭제할 사용자 ID
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    void deleteAccount(UUID userId);

    /**
     * 사용자 설정을 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 설정 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    SettingsResponse getSettings(UUID userId);

    /**
     * 사용자 설정을 수정합니다.
     * 
     * @param userId 수정할 사용자 ID
     * @param request 설정 수정 요청 정보
     * @return 수정된 설정 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    SettingsResponse updateSettings(UUID userId, SettingsUpdateRequest request);
} 