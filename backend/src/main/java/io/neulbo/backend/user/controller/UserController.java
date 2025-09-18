package io.neulbo.backend.user.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.user.dto.*;
import io.neulbo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * 사용자 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/users") // 자동으로 /api/v1/users가 됩니다
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 현재 사용자 프로필 조회
     * GET /users/me/profile
     */
    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> getProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ProfileResponse profile = userService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.error("프로필 조회 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 현재 사용자 프로필 수정
     * PUT /users/me/profile
     */
    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ProfileResponse updatedProfile = userService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            log.error("프로필 수정 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 현재 사용자 계정 정보 조회
     * GET /users/me/account
     */
    @GetMapping("/me/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> getAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            AccountResponse account = userService.getAccount(userId);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            log.error("계정 조회 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 현재 사용자 계정 정보 수정
     * PUT /users/me/account
     */
    @PutMapping("/me/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> updateAccount(@Valid @RequestBody AccountUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            AccountResponse updatedAccount = userService.updateAccount(userId, request);
            return ResponseEntity.ok(updatedAccount);
        } catch (IllegalArgumentException e) {
            log.error("계정 수정 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 현재 사용자 계정 삭제 (탈퇴)
     * DELETE /users/me
     */
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            userService.deleteAccount(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("계정 삭제 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 현재 사용자 설정 조회
     * GET /users/me/settings
     */
    @GetMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettingsResponse> getSettings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            SettingsResponse settings = userService.getSettings(userId);
            return ResponseEntity.ok(settings);
        } catch (IllegalArgumentException e) {
            log.error("설정 조회 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 현재 사용자 설정 수정
     * PUT /users/me/settings
     */
    @PutMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettingsResponse> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            SettingsResponse updatedSettings = userService.updateSettings(userId, request);
            return ResponseEntity.ok(updatedSettings);
        } catch (IllegalArgumentException e) {
            log.error("설정 수정 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 