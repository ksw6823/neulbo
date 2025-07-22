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

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

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
        addCompletedDay(LocalDate.now());
    }
    
    public void addCompletedDay(LocalDate completionDate) {
        // 이미 완료된 챌린지는 추가 진행 불가
        if (this.status == ChallengeStatus.COMPLETED) {
            return;
        }
        
        // 같은 날짜에 이미 완료 처리된 경우 중복 방지
        if (completionDate.equals(this.lastCompletedDate)) {
            return;
        }
        
        this.completedDays++;
        this.lastCompletedDate = completionDate;
        
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
        // division by zero 방지
        if (challenge.getDurationDays() <= 0) {
            // 기간이 0 이하인 경우, 완료된 날이 있으면 100%, 없으면 0% 반환
            return completedDays > 0 ? 100.0 : 0.0;
        }
        
        return (double) completedDays / challenge.getDurationDays() * 100;
    }
    
    /**
     * 주어진 날짜가 챌린지 기간 내에 있는지 확인
     */
    public boolean isDateWithinChallengePeriod(LocalDate date) {
        return !date.isBefore(this.startDate) && !date.isAfter(this.endDate);
    }
    
    /**
     * 해당 날짜에 이미 완료 처리되었는지 확인
     */
    public boolean isAlreadyCompletedOnDate(LocalDate date) {
        return date.equals(this.lastCompletedDate);
    }
    
    /**
     * 챌린지가 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == ChallengeStatus.ACTIVE;
    }
} 