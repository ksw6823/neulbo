package io.neulbo.backend.user.domain;

import io.neulbo.backend.user.dto.AccountResponse;
import io.neulbo.backend.user.dto.ProfileResponse;
import io.neulbo.backend.user.dto.SettingsResponse;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    // 포인트 관련 상수
    public static final int MAX_POINTS = 1_000_000; // 최대 포인트 한도

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(length = 100)
    private String username;

    @Column(length = 100)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Column
    private LocalDate birth;

    @Column(name = "current_points", nullable = false)
    @Builder.Default
    private Integer currentPoints = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // DTO 변환 메서드들
    public ProfileResponse toProfileResponse() {
        return ProfileResponse.builder()
                .id(this.id)
                .username(this.username)
                .profileImage(this.profileImage)
                .birth(this.birth)
                .isPrivate(this.isPrivate)
                .provider(this.provider)
                .build();
    }

    public AccountResponse toAccountResponse() {
        return AccountResponse.builder()
                .id(this.id)
                .provider(this.provider)
                .providerId(this.providerId)
                .username(this.username)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public SettingsResponse toSettingsResponse() {
        return SettingsResponse.builder()
                .id(this.id)
                .isPrivate(this.isPrivate)
                .build();
    }

    // 업데이트 메서드들
    public void updateProfile(String username, String profileImage, LocalDate birth, Boolean isPrivate) {
        if (username != null) this.username = username;
        if (profileImage != null) this.profileImage = profileImage;
        if (birth != null) this.birth = birth;
        if (isPrivate != null) this.isPrivate = isPrivate;
    }

    public void updateAccount(String username) {
        if (username != null) this.username = username;
    }

    public void updateSettings(Boolean isPrivate) {
        if (isPrivate != null) this.isPrivate = isPrivate;
    }

    public void addPoints(Integer points) {
        if (points == null) {
            throw new IllegalArgumentException("추가할 포인트는 null일 수 없습니다.");
        }
        
        if (points <= 0) {
            throw new IllegalArgumentException("추가할 포인트는 양수여야 합니다.");
        }
        
        if (points > MAX_POINTS) {
            throw new IllegalArgumentException("한 번에 추가할 수 있는 포인트는 " + MAX_POINTS + " 이하여야 합니다.");
        }
        
        // 오버플로우 검사
        if (this.currentPoints > MAX_POINTS - points) {
            throw new IllegalArgumentException("포인트 추가 후 최대 한도(" + MAX_POINTS + ")를 초과할 수 없습니다.");
        }
        
        this.currentPoints += points;
    }

    public void subtractPoints(Integer points) {
        if (points == null) {
            throw new IllegalArgumentException("차감할 포인트는 null일 수 없습니다.");
        }
        
        if (points <= 0) {
            throw new IllegalArgumentException("차감할 포인트는 양수여야 합니다.");
        }
        
        if (this.currentPoints < points) {
            throw new IllegalArgumentException("보유 포인트가 부족합니다. 현재: " + this.currentPoints + ", 필요: " + points);
        }
        
        this.currentPoints -= points;
    }

    // OAuth 관련 메서드들
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    /**
     * OAuth 사용자 생성을 위한 정적 팩토리 메서드
     */
    public static User createOAuthUser(String provider, String providerId, String email, String displayName, String fullName) {
        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .username(displayName)
                .fullName(fullName)
                .build();
    }
}
