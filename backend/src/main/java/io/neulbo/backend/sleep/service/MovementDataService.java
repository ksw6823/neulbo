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
                .map(stat -> {
                    try {
                        return new HourlyMovementPattern(
                                safeCastToLocalDateTime(stat, 0, "hour"),
                                safeCastToLong(stat, 1, "movementCount"),
                                safeCastToDouble(stat, 2, "avgIntensity")
                        );
                    } catch (Exception e) {
                        log.error("시간대별 움직임 패턴 변환 실패: sessionId={}, data={}", sessionId, stat, e);
                        throw new BusinessException(ErrorCode.SLEEP_ANALYSIS_FAILED);
                    }
                })
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

    /**
     * Object[] 배열에서 LocalDateTime을 안전하게 캐스팅
     */
    private LocalDateTime safeCastToLocalDateTime(Object[] data, int index, String fieldName) {
        validateArrayAccess(data, index, fieldName);
        
        Object value = data[index];
        if (value == null) {
            throw new IllegalArgumentException(String.format("%s 값이 null입니다 (index: %d)", fieldName, index));
        }
        
        if (!(value instanceof LocalDateTime)) {
            throw new ClassCastException(String.format(
                "%s 값을 LocalDateTime으로 캐스팅할 수 없습니다. 실제 타입: %s, 값: %s (index: %d)", 
                fieldName, value.getClass().getSimpleName(), value, index));
        }
        
        return (LocalDateTime) value;
    }

    /**
     * Object[] 배열에서 Long을 안전하게 캐스팅
     */
    private Long safeCastToLong(Object[] data, int index, String fieldName) {
        validateArrayAccess(data, index, fieldName);
        
        Object value = data[index];
        if (value == null) {
            log.warn("{} 값이 null입니다. 기본값 0을 사용합니다 (index: {})", fieldName, index);
            return 0L;
        }
        
        if (!(value instanceof Number)) {
            throw new ClassCastException(String.format(
                "%s 값을 Number로 캐스팅할 수 없습니다. 실제 타입: %s, 값: %s (index: %d)", 
                fieldName, value.getClass().getSimpleName(), value, index));
        }
        
        return ((Number) value).longValue();
    }

    /**
     * Object[] 배열에서 Double을 안전하게 캐스팅
     */
    private Double safeCastToDouble(Object[] data, int index, String fieldName) {
        validateArrayAccess(data, index, fieldName);
        
        Object value = data[index];
        if (value == null) {
            log.warn("{} 값이 null입니다. 기본값 0.0을 사용합니다 (index: {})", fieldName, index);
            return 0.0;
        }
        
        if (!(value instanceof Number)) {
            throw new ClassCastException(String.format(
                "%s 값을 Number로 캐스팅할 수 없습니다. 실제 타입: %s, 값: %s (index: %d)", 
                fieldName, value.getClass().getSimpleName(), value, index));
        }
        
        return ((Number) value).doubleValue();
    }

    /**
     * 배열 접근 유효성 검증
     */
    private void validateArrayAccess(Object[] data, int index, String fieldName) {
        if (data == null) {
            throw new IllegalArgumentException("데이터 배열이 null입니다");
        }
        
        if (index < 0) {
            throw new IllegalArgumentException(String.format(
                "잘못된 배열 인덱스입니다. %s 필드의 인덱스: %d (음수 불가)", fieldName, index));
        }
        
        if (index >= data.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                "배열 인덱스가 범위를 벗어났습니다. %s 필드의 인덱스: %d, 배열 크기: %d", 
                fieldName, index, data.length));
        }
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