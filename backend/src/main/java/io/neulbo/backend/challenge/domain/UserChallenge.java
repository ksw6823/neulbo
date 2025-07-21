package io.neulbo.backend.challenge.domain;

import io.neulbo.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_challenges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "completed_days", nullable = false)
    @Builder.Default
    private Integer completedDays = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 챌린지 상태 Enum
    public enum ChallengeStatus {
        ACTIVE,      // 진행 중
        COMPLETED,   // 완료
        FAILED       // 실패
    }

    // 비즈니스 메서드
    public void addCompletedDay() {
        this.completedDays++;
        
        // 챌린지 완료 체크
        if (this.completedDays >= this.challenge.getDurationDays()) {
            this.status = ChallengeStatus.COMPLETED;
        }
    }

    public void markAsFailed() {
        this.status = ChallengeStatus.FAILED;
    }

    public boolean isCompleted() {
        return this.status == ChallengeStatus.COMPLETED;
    }

    public double getProgressPercentage() {
        return (double) completedDays / challenge.getDurationDays() * 100;
    }
} 