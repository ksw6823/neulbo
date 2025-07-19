package io.neulbo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계정 정보 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    
    private UUID id;
    private String provider;
    private String providerId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 