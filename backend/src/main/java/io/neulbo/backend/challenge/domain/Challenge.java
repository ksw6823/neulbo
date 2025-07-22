package io.neulbo.backend.challenge.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChallengeType type;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays; // 7, 14, 30일

    @Column(name = "target_value", nullable = false)
    private Integer targetValue; // 목표값 (분 단위)

    @Column(name = "reward_points", nullable = false)
    @Builder.Default
    private Integer rewardPoints = 100;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 챌린지 타입 Enum
    public enum ChallengeType {
        SLEEP_TIME,      // 수면 시간 챌린지
        CONSISTENCY,     // 수면 일관성 챌린지  
        WAKE_TIME        // 기상 시간 챌린지
    }
} 