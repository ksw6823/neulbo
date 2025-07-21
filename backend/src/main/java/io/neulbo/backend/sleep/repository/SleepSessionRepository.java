package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {

    // 특정 사용자의 수면 세션 목록 조회 (페이징)
    Page<SleepSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 특정 사용자의 최근 수면 세션 조회
    Optional<SleepSession> findTopByUserOrderByCreatedAtDesc(User user);

    // 특정 사용자의 진행 중인 수면 세션 조회
    Optional<SleepSession> findByUserAndSessionStatus(User user, SessionStatus status);

    // 특정 사용자의 완료된 수면 세션 목록
    List<SleepSession> findByUserAndSessionStatusOrderByCreatedAtDesc(User user, SessionStatus status);

    // 특정 기간 내 수면 세션 조회
    @Query("SELECT s FROM SleepSession s WHERE s.user = :user AND s.sleepStartTime >= :startDate AND s.sleepStartTime <= :endDate ORDER BY s.sleepStartTime DESC")
    List<SleepSession> findByUserAndDateRange(@Param("user") User user, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);

    // 특정 사용자의 주간 수면 세션 조회
    @Query("SELECT s FROM SleepSession s WHERE s.user = :user AND s.sleepStartTime >= :weekStart ORDER BY s.sleepStartTime DESC")
    List<SleepSession> findByUserAndLastWeek(@Param("user") User user, @Param("weekStart") LocalDateTime weekStart);

    // 특정 사용자의 월간 수면 세션 조회
    @Query("SELECT s FROM SleepSession s WHERE s.user = :user AND s.sleepStartTime >= :monthStart ORDER BY s.sleepStartTime DESC")
    List<SleepSession> findByUserAndLastMonth(@Param("user") User user, @Param("monthStart") LocalDateTime monthStart);

    // 수면 효율이 특정 값 이상인 세션들
    @Query("SELECT s FROM SleepSession s WHERE s.user = :user AND s.sleepEfficiencyPercentage >= :efficiency ORDER BY s.sleepStartTime DESC")
    List<SleepSession> findByUserAndSleepEfficiencyGreaterThanEqual(@Param("user") User user, @Param("efficiency") Double efficiency);

    // 특정 사용자의 평균 수면 시간 계산
    @Query("SELECT AVG(s.actualSleepDurationMinutes) FROM SleepSession s WHERE s.user = :user AND s.sessionStatus = 'COMPLETED'")
    Optional<Double> findAverageSleepDurationByUser(@Param("user") User user);

    // 특정 사용자의 평균 수면 효율 계산
    @Query("SELECT AVG(s.sleepEfficiencyPercentage) FROM SleepSession s WHERE s.user = :user AND s.sessionStatus = 'COMPLETED'")
    Optional<Double> findAverageSleepEfficiencyByUser(@Param("user") User user);

    // 중단된 세션들 조회 (정리용)
    @Query("SELECT s FROM SleepSession s WHERE s.sessionStatus = 'IN_PROGRESS' AND s.createdAt < :cutoffTime")
    List<SleepSession> findStaleInProgressSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
} 