package io.neulbo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 사용자 프로필 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    
    private UUID id;
    private String username;
    private String profileImage;
    private LocalDate birth;
    private Boolean isPrivate;
    private String provider;
} 