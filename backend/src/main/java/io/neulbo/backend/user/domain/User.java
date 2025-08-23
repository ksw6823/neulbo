package io.neulbo.backend.user.domain;

import io.neulbo.backend.friends.domain.Friends;
import io.neulbo.backend.user.dto.AccountResponse;
import io.neulbo.backend.user.dto.ProfileResponse;
import io.neulbo.backend.user.dto.SettingsResponse;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(length = 100)
    private String username;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Column
    private LocalDate birth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // follow 일대다 관계
    @OneToMany(mappedBy = "fromUser", fetch = FetchType.LAZY)
    private List<Friends> followings;

    @OneToMany(mappedBy = "toUser", fetch = FetchType.LAZY)
    private List<Friends> followers;

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
}
