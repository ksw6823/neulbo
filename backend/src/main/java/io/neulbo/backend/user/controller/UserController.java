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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 프로필 조회
     * GET /users/profile
     */
    @GetMapping("/profile")
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
     * 사용자 프로필 수정
     * PUT /users/profile
     */
    @PutMapping("/profile")
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
     * 사용자 계정 정보 조회
     * GET /users/account
     */
    @GetMapping("/account")
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
     * 사용자 계정 정보 수정
     * PUT /users/account
     */
    @PutMapping("/account")
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
     * 사용자 계정 삭제 (탈퇴)
     * DELETE /users/account
     */
    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            userService.deleteAccount(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("계정 삭제 실패 - User ID: {}, Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 사용자 설정 조회
     * GET /users/settings
     */
    @GetMapping("/settings")
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
     * 사용자 설정 수정
     * PUT /users/settings
     */
    @PutMapping("/settings")
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