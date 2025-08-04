package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.MovementData;
import io.neulbo.backend.sleep.domain.MovementType;
import io.neulbo.backend.sleep.domain.SleepSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    // 특정 움직임 타입들의 데이터 조회
    @Query("SELECT m FROM MovementData m WHERE m.sleepSession = :session AND m.movementType IN :movementTypes ORDER BY m.timestamp ASC")
    List<MovementData> findByMovementTypesAndSleepSession(@Param("session") SleepSession session, 
                                                          @Param("movementTypes") Collection<MovementType> movementTypes);

    // 특정 세션의 움직임 강도 평균 계산
    @Query("SELECT AVG(m.movementIntensity) FROM MovementData m WHERE m.sleepSession = :session")
    Double findAverageMovementIntensityBySleepSession(@Param("session") SleepSession session);

    // 특정 세션의 총 움직임 수 계산 (제외할 움직임 타입들 지정)
    @Query("SELECT COUNT(m) FROM MovementData m WHERE m.sleepSession = :session AND m.movementType NOT IN :excludedTypes")
    Long countMovementsBySleepSessionExcluding(@Param("session") SleepSession session, 
                                               @Param("excludedTypes") Collection<MovementType> excludedTypes);

    // 기존 호환성을 위한 편의 메서드들
    
    /**
     * 강한 움직임 데이터 조회 (MODERATE_MOVEMENT, STRONG_MOVEMENT)
     * @param session 수면 세션
     * @return 강한 움직임 데이터 목록
     */
    default List<MovementData> findStrongMovementsBySleepSession(SleepSession session) {
        return findByMovementTypesAndSleepSession(session, 
            List.of(MovementType.MODERATE_MOVEMENT, MovementType.STRONG_MOVEMENT));
    }
    
    /**
     * 특정 세션의 총 움직임 수 계산 (STILL 제외)
     * @param session 수면 세션
     * @return 움직임 수
     */
    default Long countMovementsBySleepSession(SleepSession session) {
        return countMovementsBySleepSessionExcluding(session, List.of(MovementType.STILL));
    }

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