package io.neulbo.backend.user.service;

import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.dto.*;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public User findUserById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다");
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 조회 실패 - User ID: {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
                });
    }

    @Override
    public ProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        log.info("프로필 조회 성공 - User ID: {}", userId);
        return user.toProfileResponse();
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = findUserById(userId);
        
        user.updateProfile(
                request.getUsername(),
                request.getProfileImage(),
                request.getBirth(),
                request.getIsPrivate()
        );
        
        User savedUser = userRepository.save(user);
        log.info("프로필 수정 성공 - User ID: {}", userId);
        return savedUser.toProfileResponse();
    }

    @Override
    public AccountResponse getAccount(UUID userId) {
        User user = findUserById(userId);
        log.info("계정 조회 성공 - User ID: {}", userId);
        return user.toAccountResponse();
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(UUID userId, AccountUpdateRequest request) {
        User user = findUserById(userId);
        
        user.updateAccount(request.getUsername());
        
        User savedUser = userRepository.save(user);
        log.info("계정 수정 성공 - User ID: {}", userId);
        return savedUser.toAccountResponse();
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findUserById(userId);
        
        userRepository.delete(user);
        log.info("계정 삭제 성공 - User ID: {}", userId);
    }

    @Override
    public SettingsResponse getSettings(UUID userId) {
        User user = findUserById(userId);
        log.info("설정 조회 성공 - User ID: {}", userId);
        return user.toSettingsResponse();
    }

    @Override
    @Transactional
    public SettingsResponse updateSettings(UUID userId, SettingsUpdateRequest request) {
        User user = findUserById(userId);
        
        user.updateSettings(request.getIsPrivate());
        
        User savedUser = userRepository.save(user);
        log.info("설정 수정 성공 - User ID: {}", userId);
        return savedUser.toSettingsResponse();
    }
} 