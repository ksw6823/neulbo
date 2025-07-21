package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.MovementData;
import io.neulbo.backend.sleep.domain.MovementType;
import io.neulbo.backend.sleep.domain.SleepSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovementDataRepository extends JpaRepository<MovementData, Long> {

    // 특정 수면 세션의 움직임 데이터 조회
    List<MovementData> findBySleepSessionOrderByTimestampAsc(SleepSession sleepSession);

    // 특정 시간 범위의 움직임 데이터 조회
    @Query("SELECT m FROM MovementData m WHERE m.sleepSession = :session AND m.timestamp >= :startTime AND m.timestamp <= :endTime ORDER BY m.timestamp ASC")
    List<MovementData> findBySleepSessionAndTimeRange(@Param("session") SleepSession session,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    // 특정 움직임 타입의 데이터만 조회
    List<MovementData> findBySleepSessionAndMovementTypeOrderByTimestampAsc(SleepSession sleepSession, MovementType movementType);

    // 강한 움직임 데이터만 조회
    @Query("SELECT m FROM MovementData m WHERE m.sleepSession = :session AND m.movementType IN ('MODERATE_MOVEMENT', 'STRONG_MOVEMENT') ORDER BY m.timestamp ASC")
    List<MovementData> findStrongMovementsBySleepSession(@Param("session") SleepSession session);

    // 특정 세션의 움직임 강도 평균 계산
    @Query("SELECT AVG(m.movementIntensity) FROM MovementData m WHERE m.sleepSession = :session")
    Double findAverageMovementIntensityBySleepSession(@Param("session") SleepSession session);

    // 특정 세션의 총 움직임 수 계산
    @Query("SELECT COUNT(m) FROM MovementData m WHERE m.sleepSession = :session AND m.movementType != 'STILL'")
    Long countMovementsBySleepSession(@Param("session") SleepSession session);

    // 특정 시간 간격별 움직임 통계
    @Query("SELECT COUNT(m) FROM MovementData m WHERE m.sleepSession = :session AND m.timestamp >= :startTime AND m.timestamp < :endTime")
    Long countMovementsByTimeInterval(@Param("session") SleepSession session,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    // 가장 활발했던 시간대 조회 (최대 움직임 강도)
    @Query("SELECT m FROM MovementData m WHERE m.sleepSession = :session ORDER BY m.movementIntensity DESC")
    List<MovementData> findMostActiveMovementsBySleepSession(@Param("session") SleepSession session);

    // 특정 세션의 시간대별 움직임 패턴 분석용 데이터
    @Query("SELECT DATE_TRUNC('hour', m.timestamp) as hour, COUNT(m) as movementCount, AVG(m.movementIntensity) as avgIntensity " +
           "FROM MovementData m WHERE m.sleepSession = :session " +
           "GROUP BY DATE_TRUNC('hour', m.timestamp) ORDER BY hour")
    List<Object[]> findHourlyMovementStatsBySleepSession(@Param("session") SleepSession session);
} 