package io.neulbo.backend.sleep.service;

import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.sleep.domain.MovementData;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.dto.request.MovementDataRequest;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MovementDataService {

    private final MovementDataRepository movementDataRepository;
    private final SleepSessionRepository sleepSessionRepository;

    /**
     * 움직임 데이터 일괄 저장
     */
    @Transactional
    public void saveMovementData(User user, MovementDataRequest request) {
        log.info("사용자 {}의 움직임 데이터 저장 요청 - {} 개 데이터", user.getId(), request.getMovementData().size());

        // 현재 진행 중인 수면 세션 조회
        SleepSession currentSession = sleepSessionRepository.findByUserAndSessionStatus(user, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        // 움직임 데이터 검증 및 변환
        List<MovementData> movementDataList = request.getMovementData().stream()
                .map(entry -> {
                    try {
                        return MovementData.create(
                                entry.getTimestamp(),
                                entry.getAccelerationX(),
                                entry.getAccelerationY(),
                                entry.getAccelerationZ()
                        );
                    } catch (Exception e) {
                        log.error("움직임 데이터 생성 실패: {}", entry, e);
                        throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DATA);
                    }
                })
                .collect(Collectors.toList());

        // 세션에 움직임 데이터 추가
        for (MovementData movementData : movementDataList) {
            currentSession.addMovementData(movementData);
        }

        // 일괄 저장
        movementDataRepository.saveAll(movementDataList);
        sleepSessionRepository.save(currentSession);

        log.info("움직임 데이터 저장 완료: sessionId={}, 저장된 데이터 수={}", 
                currentSession.getId(), movementDataList.size());
    }

    /**
     * 특정 세션의 움직임 데이터 조회
     */
    public List<MovementData> getMovementDataBySession(User user, Long sessionId) {
        SleepSession session = getSleepSessionWithAccessCheck(user, sessionId);
        return movementDataRepository.findBySleepSessionOrderByTimestampAsc(session);
    }

    /**
     * 특정 시간 범위의 움직임 데이터 조회
     */
    public List<MovementData> getMovementDataByTimeRange(User user, Long sessionId, 
                                                        LocalDateTime startTime, LocalDateTime endTime) {
        SleepSession session = getSleepSessionWithAccessCheck(user, sessionId);
        return movementDataRepository.findBySleepSessionAndTimeRange(session, startTime, endTime);
    }

    /**
     * 특정 세션의 움직임 통계 계산
     */
    public MovementStatistics calculateMovementStatistics(User user, Long sessionId) {
        SleepSession session = getSleepSessionWithAccessCheck(user, sessionId);

        Double averageIntensity = movementDataRepository.findAverageMovementIntensityBySleepSession(session);
        Long totalMovements = movementDataRepository.countMovementsBySleepSession(session);
        List<MovementData> strongMovements = movementDataRepository.findStrongMovementsBySleepSession(session);
        List<Object[]> hourlyStats = movementDataRepository.findHourlyMovementStatsBySleepSession(session);

        return new MovementStatistics(
                averageIntensity != null ? averageIntensity : 0.0,
                totalMovements != null ? totalMovements : 0L,
                (long) strongMovements.size(),
                hourlyStats
        );
    }

    /**
     * 움직임 패턴 분석 (시간대별)
     */
    public List<HourlyMovementPattern> analyzeHourlyMovementPattern(User user, Long sessionId) {
        SleepSession session = getSleepSessionWithAccessCheck(user, sessionId);
        List<Object[]> hourlyStats = movementDataRepository.findHourlyMovementStatsBySleepSession(session);

        return hourlyStats.stream()
                .map(stat -> new HourlyMovementPattern(
                        (LocalDateTime) stat[0],  // hour
                        ((Number) stat[1]).longValue(),  // movementCount
                        ((Number) stat[2]).doubleValue() // avgIntensity
                ))
                .collect(Collectors.toList());
    }

    private SleepSession getSleepSessionWithAccessCheck(User user, Long sessionId) {
        SleepSession session = sleepSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        // 본인의 세션인지 확인
        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return session;
    }

    // 내부 클래스들
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class MovementStatistics {
        private final Double averageMovementIntensity;
        private final Long totalMovementCount;
        private final Long strongMovementCount;
        private final List<Object[]> hourlyStatistics;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class HourlyMovementPattern {
        private final LocalDateTime hour;
        private final Long movementCount;
        private final Double averageIntensity;
    }
} 