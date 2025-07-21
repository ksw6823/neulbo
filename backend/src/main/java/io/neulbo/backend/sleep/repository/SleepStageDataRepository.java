package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SleepStage;
import io.neulbo.backend.sleep.domain.SleepStageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepStageDataRepository extends JpaRepository<SleepStageData, Long> {

    // 특정 수면 세션의 수면 단계 데이터 조회
    List<SleepStageData> findBySleepSessionOrderByStageStartTimeAsc(SleepSession sleepSession);

    // 특정 수면 세션의 특정 수면 단계 데이터만 조회
    List<SleepStageData> findBySleepSessionAndSleepStageOrderByStageStartTimeAsc(SleepSession sleepSession, SleepStage sleepStage);

    // 현재 진행 중인 수면 단계 조회 (종료 시간이 null인 것)
    Optional<SleepStageData> findBySleepSessionAndStageEndTimeIsNull(SleepSession sleepSession);

    // 특정 시간 범위의 수면 단계 데이터 조회
    @Query("SELECT s FROM SleepStageData s WHERE s.sleepSession = :session AND s.stageStartTime >= :startTime AND s.stageStartTime <= :endTime ORDER BY s.stageStartTime ASC")
    List<SleepStageData> findBySleepSessionAndTimeRange(@Param("session") SleepSession session,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    // 특정 수면 세션의 각 단계별 총 시간 계산
    @Query("SELECT s.sleepStage, SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.durationMinutes IS NOT NULL GROUP BY s.sleepStage")
    List<Object[]> findStageDurationBySleepSession(@Param("session") SleepSession session);

    // 깊은 잠 단계의 총 시간 계산
    @Query("SELECT SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage = 'DEEP_SLEEP' AND s.durationMinutes IS NOT NULL")
    Optional<Long> findTotalDeepSleepDurationBySleepSession(@Param("session") SleepSession session);

    // REM 수면 단계의 총 시간 계산
    @Query("SELECT SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage = 'REM' AND s.durationMinutes IS NOT NULL")
    Optional<Long> findTotalREMSleepDurationBySleepSession(@Param("session") SleepSession session);

    // 얕은 잠 단계의 총 시간 계산
    @Query("SELECT SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage = 'LIGHT_SLEEP' AND s.durationMinutes IS NOT NULL")
    Optional<Long> findTotalLightSleepDurationBySleepSession(@Param("session") SleepSession session);

    // 깨어있었던 총 시간 계산
    @Query("SELECT SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage = 'AWAKE' AND s.durationMinutes IS NOT NULL")
    Optional<Long> findTotalAwakeDurationBySleepSession(@Param("session") SleepSession session);

    // 각 수면 단계의 평균 지속 시간
    @Query("SELECT s.sleepStage, AVG(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.durationMinutes IS NOT NULL GROUP BY s.sleepStage")
    List<Object[]> findAverageStageDurationBySleepSession(@Param("session") SleepSession session);

    // 수면 단계 변화 횟수 계산
    @Query("SELECT COUNT(s) FROM SleepStageData s WHERE s.sleepSession = :session")
    Long countStageChangesBySleepSession(@Param("session") SleepSession session);

    // 가장 긴 연속 깊은 잠 구간 찾기
    @Query("SELECT MAX(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage = 'DEEP_SLEEP' AND s.durationMinutes IS NOT NULL")
    Optional<Integer> findLongestDeepSleepPeriodBySleepSession(@Param("session") SleepSession session);

    // 특정 시간에 해당하는 수면 단계 조회
    @Query("SELECT s FROM SleepStageData s WHERE s.sleepSession = :session AND s.stageStartTime <= :time AND (s.stageEndTime IS NULL OR s.stageEndTime > :time)")
    Optional<SleepStageData> findSleepStageAtTime(@Param("session") SleepSession session, @Param("time") LocalDateTime time);

    // 수면 효율 계산을 위한 실제 수면 시간 (AWAKE 제외)
    @Query("SELECT SUM(s.durationMinutes) FROM SleepStageData s WHERE s.sleepSession = :session AND s.sleepStage != 'AWAKE' AND s.durationMinutes IS NOT NULL")
    Optional<Long> findTotalActualSleepTimeBySleepSession(@Param("session") SleepSession session);
} 