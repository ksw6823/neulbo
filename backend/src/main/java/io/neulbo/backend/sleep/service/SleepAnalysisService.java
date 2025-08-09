package io.neulbo.backend.sleep.service;

import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.sleep.domain.*;
import io.neulbo.backend.sleep.dto.response.SleepStatisticsResponse;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.sleep.repository.SleepStageDataRepository;
import io.neulbo.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SleepAnalysisService {

    private final SleepSessionRepository sleepSessionRepository;
    private final MovementDataRepository movementDataRepository;
    private final SleepStageDataRepository sleepStageDataRepository;

    /**
     * 완료된 수면 세션 분석 (수면 단계 분석)
     */
    @Transactional
    public void analyzeSleepSession(Long sessionId) {
        log.info("수면 세션 분석 시작: sessionId={}", sessionId);

        SleepSession session = sleepSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        if (!session.isCompleted()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            // 기존 수면 단계 데이터 벌크 삭제 (재분석인 경우)
            // 메모리에 로드하지 않고 직접 데이터베이스에서 삭제하여 성능 최적화
            int deletedCount = sleepStageDataRepository.deleteBySleepSession(session);
            if (deletedCount > 0) {
                log.info("기존 수면 단계 데이터 벌크 삭제: {} 개", deletedCount);
            }

            // 움직임 데이터 조회
            List<MovementData> movementData = movementDataRepository.findBySleepSessionOrderByTimestampAsc(session);
            
            if (movementData.isEmpty()) {
                log.warn("움직임 데이터가 없어 기본 수면 단계 생성: sessionId={}", sessionId);
                createDefaultSleepStages(session);
            } else {
                // 움직임 기반 수면 단계 분석
                analyzeAndCreateSleepStages(session, movementData);
            }

            log.info("수면 세션 분석 완료: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("수면 세션 분석 실패: sessionId={}", sessionId, e);
            throw new BusinessException(ErrorCode.SLEEP_ANALYSIS_FAILED);
        }
    }

    /**
     * 사용자의 수면 통계 조회
     */
    public SleepStatisticsResponse getSleepStatistics(User user) {
        List<SleepSession> completedSessions = sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(
                user, SessionStatus.COMPLETED);

        if (completedSessions.isEmpty()) {
            return createEmptyStatistics();
        }

        // 기본 통계 계산
        Double avgSleepDuration = sleepSessionRepository.findAverageSleepDurationByUser(user).orElse(0.0);
        Double avgSleepEfficiency = sleepSessionRepository.findAverageSleepEfficiencyByUser(user).orElse(0.0);

        // 수면 단계별 통계 계산
        Map<String, Long> stageDistribution = calculateStageDistribution(completedSessions);
        
        // 각 단계별 평균 비율 계산
        Double avgDeepSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.DEEP_SLEEP);
        Double avgREMSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.REM);
        Double avgLightSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.LIGHT_SLEEP);

        // 기타 통계
        Integer avgWakeUpCount = (int) completedSessions.stream()
                .mapToInt(s -> s.getWakeUpCount() != null ? s.getWakeUpCount() : 0)
                .average().orElse(0.0);

        Double avgMovementCount = completedSessions.stream()
                .mapToInt(s -> s.getTotalMovementCount() != null ? s.getTotalMovementCount() : 0)
                .average().orElse(0.0);

        // 전체 수면 세션 수 조회
        long totalSessions = sleepSessionRepository.countByUser(user);

        return new SleepStatisticsResponse(
                avgSleepDuration,
                avgSleepEfficiency,
                (int) totalSessions,  // 전체 수면 세션 수
                completedSessions.size(),  // 완료된 세션 수
                stageDistribution,
                avgDeepSleepPercentage,
                avgREMSleepPercentage,
                avgLightSleepPercentage,
                avgWakeUpCount,
                avgMovementCount
        );
    }

    /**
     * 특정 기간의 수면 통계 조회
     */
    public SleepStatisticsResponse getSleepStatisticsForPeriod(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<SleepSession> allSessionsInPeriod = sleepSessionRepository.findByUserAndDateRange(user, startDate, endDate);
        List<SleepSession> completedSessions = allSessionsInPeriod.stream()
                .filter(SleepSession::isCompleted)
                .collect(Collectors.toList());

        return calculateStatisticsForSessions(completedSessions, allSessionsInPeriod.size());
    }

    /**
     * 움직임 기반 수면 단계 분석 (간단한 규칙 기반)
     * 성능 최적화: O(n²) → O(n + m) 복잡도로 개선 (n: movementData 크기, m: 간격 수)
     * 
     * NOTE: 테스트에서 접근 가능하도록 package-private으로 설정됨
     */
    void analyzeAndCreateSleepStages(SleepSession session, List<MovementData> movementData) {
        LocalDateTime sleepStart = session.getSleepStartTime();
        LocalDateTime sleepEnd = session.getSleepEndTime();
        
        // 10분 단위로 분석
        long totalMinutes = ChronoUnit.MINUTES.between(sleepStart, sleepEnd);
        int intervalMinutes = 10;
        
        List<SleepStageData> stageDataList = new ArrayList<>();
        
        // movementData가 시간순으로 정렬되어 있다고 가정하고 인덱스 기반 접근법 사용
        // O(n²) 복잡도를 O(n + m)으로 개선 (중복 필터링 제거)
        int movementDataIndex = 0;
        
        for (int i = 0; i < totalMinutes; i += intervalMinutes) {
            LocalDateTime intervalStart = sleepStart.plusMinutes(i);
            LocalDateTime intervalEnd = sleepStart.plusMinutes(Math.min(i + intervalMinutes, totalMinutes));
            
            // 해당 구간의 움직임 데이터를 인덱스 기반으로 효율적으로 수집
            List<MovementData> intervalMovements = collectMovementsForInterval(
                    movementData, intervalStart, intervalEnd, movementDataIndex);
            
            // 다음 간격을 위해 인덱스 업데이트: 현재 간격을 지나간 데이터들은 건너뛰기
            movementDataIndex = updateMovementDataIndex(movementData, intervalEnd, movementDataIndex);
            
            // 수면 단계 결정
            SleepStage stage = determineSleepStage(intervalMovements, intervalStart, sleepStart);
            double confidence = calculateConfidence(intervalMovements);
            
            // 수면 단계 데이터 생성
            SleepStageData stageData = SleepStageData.create(stage, intervalStart, confidence);
            if (!stageData.tryEndStage(intervalEnd)) {
                // 종료 시간이 유효하지 않은 경우 로깅하고 기본 처리
                log.warn("Invalid end time for sleep stage: start={}, end={}, skipping interval", 
                    intervalStart, intervalEnd);
                continue; // 이 구간은 건너뛰고 다음 구간으로
            }
            stageData.assignToSleepSession(session);
            
            // 움직임 카운트 설정 (이미 수집된 intervalMovements 사용)
            int movementCount = (int) intervalMovements.stream()
                    .filter(MovementData::hasMovement)
                    .count();
            for (int j = 0; j < movementCount; j++) {
                stageData.incrementMovementCount();
            }
            
            stageDataList.add(stageData);
        }
        
        sleepStageDataRepository.saveAll(stageDataList);
        log.info("수면 단계 분석 완료: sessionId={}, 생성된 단계 수={}", session.getId(), stageDataList.size());
    }

    /**
     * 특정 시간 간격에 해당하는 움직임 데이터를 인덱스 기반으로 효율적으로 수집
     * O(n) 시간 복잡도로 해당 간격의 데이터만 수집
     * 
     * NOTE: 테스트에서 접근 가능하도록 package-private으로 설정됨
     * 
     * @param movementData 전체 움직임 데이터 (시간순 정렬된 상태)
     * @param intervalStart 간격 시작 시간
     * @param intervalEnd 간격 종료 시간
     * @param startIndex 검색을 시작할 인덱스
     * @return 해당 간격에 속하는 움직임 데이터 리스트
     */
    List<MovementData> collectMovementsForInterval(List<MovementData> movementData, 
                                                  LocalDateTime intervalStart, 
                                                  LocalDateTime intervalEnd, 
                                                  int startIndex) {
        List<MovementData> intervalMovements = new ArrayList<>();
        
        // startIndex부터 시작하여 해당 간격에 속하는 데이터만 수집
        for (int i = startIndex; i < movementData.size(); i++) {
            MovementData movement = movementData.get(i);
            LocalDateTime timestamp = movement.getTimestamp();
            
            // 간격 시작 시간 이전의 데이터는 건너뛰기
            if (timestamp.isBefore(intervalStart)) {
                continue;
            }
            
            // 간격 종료 시간 이후의 데이터는 중단 (이후 데이터는 다음 간격에서 처리)
            if (!timestamp.isBefore(intervalEnd)) {
                break;
            }
            
            // 간격에 포함되는 데이터 수집
            intervalMovements.add(movement);
        }
        
        return intervalMovements;
    }
    
    /**
     * 다음 간격을 위해 movementData 인덱스를 업데이트
     * 현재 간격을 지나간 데이터들은 건너뛰어 중복 확인을 방지
     * 
     * NOTE: 테스트에서 접근 가능하도록 package-private으로 설정됨
     * 
     * @param movementData 전체 움직임 데이터
     * @param intervalEnd 현재 간격의 종료 시간
     * @param currentIndex 현재 인덱스
     * @return 다음 간격에서 시작할 인덱스
     */
    int updateMovementDataIndex(List<MovementData> movementData, 
                               LocalDateTime intervalEnd, 
                               int currentIndex) {
        // 현재 간격을 지나간 데이터들을 건너뛰어 다음 간격에서 효율적으로 시작
        while (currentIndex < movementData.size()) {
            LocalDateTime timestamp = movementData.get(currentIndex).getTimestamp();
            
            // 현재 간격 종료 시간 이전의 데이터는 이미 처리됨
            if (timestamp.isBefore(intervalEnd)) {
                currentIndex++;
            } else {
                // 다음 간격에서 처리할 데이터에 도달하면 중단
                break;
            }
        }
        
        return currentIndex;
    }

    /**
     * 움직임 데이터를 기반으로 수면 단계 결정 (간단한 규칙)
     */
    private SleepStage determineSleepStage(List<MovementData> movements, LocalDateTime currentTime, LocalDateTime sleepStart) {
        if (movements.isEmpty()) {
            return SleepStage.DEEP_SLEEP; // 움직임이 없으면 깊은 잠으로 간주
        }

        // 움직임 강도 계산
        double avgIntensity = movements.stream()
                .mapToDouble(MovementData::getMovementIntensity)
                .average()
                .orElse(0.0);

        long strongMovements = movements.stream()
                .filter(MovementData::isStrongMovement)
                .count();

        // 수면 시작 후 경과 시간
        long minutesFromStart = ChronoUnit.MINUTES.between(sleepStart, currentTime);

        // 간단한 규칙 기반 분류
        if (strongMovements > movements.size() * 0.3) {
            return SleepStage.AWAKE; // 강한 움직임이 30% 이상이면 깨어있음
        } else if (avgIntensity > 2.0) {
            return SleepStage.LIGHT_SLEEP; // 평균 강도가 높으면 얕은 잠
        } else if (minutesFromStart > 60 && minutesFromStart < 240) {
            // 수면 시작 1-4시간 사이에는 깊은 잠 가능성 높음
            return avgIntensity < 0.5 ? SleepStage.DEEP_SLEEP : SleepStage.LIGHT_SLEEP;
        } else if (minutesFromStart > 240) {
            // 4시간 이후에는 REM 수면 가능성 있음
            return avgIntensity < 1.0 ? SleepStage.REM : SleepStage.LIGHT_SLEEP;
        } else {
            return SleepStage.LIGHT_SLEEP; // 기본값
        }
    }

    /**
     * 분석 신뢰도 계산
     */
    private double calculateConfidence(List<MovementData> movements) {
        if (movements.isEmpty()) {
            return 0.5; // 중간 신뢰도
        }

        // 움직임 데이터의 일관성을 기반으로 신뢰도 계산
        double avgIntensity = movements.stream()
                .mapToDouble(MovementData::getMovementIntensity)
                .average()
                .orElse(0.0);

        double variance = movements.stream()
                .mapToDouble(m -> Math.pow(m.getMovementIntensity() - avgIntensity, 2))
                .average()
                .orElse(0.0);

        // 분산이 낮을수록 신뢰도가 높음
        return Math.max(0.1, Math.min(0.9, 1.0 - (variance / 10.0)));
    }

    /**
     * 기본 수면 단계 생성 (움직임 데이터가 없는 경우)
     * 원자성과 데이터 일관성을 보장하기 위해 모든 단계를 검증한 후 일괄 저장
     */
    private void createDefaultSleepStages(SleepSession session) {
        LocalDateTime sleepStart = session.getSleepStartTime();
        LocalDateTime sleepEnd = session.getSleepEndTime();
        long totalMinutes = ChronoUnit.MINUTES.between(sleepStart, sleepEnd);

        // 전체 수면을 3단계로 나눔: 얕은잠 -> 깊은잠 -> 얕은잠
        long phase1Duration = totalMinutes * 30 / 100; // 30%
        long phase2Duration = totalMinutes * 40 / 100; // 40%
        long phase3Duration = totalMinutes - phase1Duration - phase2Duration; // 나머지

        // 모든 단계를 먼저 생성하고 검증
        List<SleepStageCreationResult> stageResults = new ArrayList<>();
        
        // Phase 1: 얕은 잠
        LocalDateTime phase1End = sleepStart.plusMinutes(phase1Duration);
        stageResults.add(createAndValidateStage(
                SleepStage.LIGHT_SLEEP, sleepStart, phase1End, 0.7, "Phase 1 (Light Sleep)", session));

        // Phase 2: 깊은 잠
        LocalDateTime phase2Start = sleepStart.plusMinutes(phase1Duration);
        LocalDateTime phase2End = sleepStart.plusMinutes(phase1Duration + phase2Duration);
        stageResults.add(createAndValidateStage(
                SleepStage.DEEP_SLEEP, phase2Start, phase2End, 0.8, "Phase 2 (Deep Sleep)", session));

        // Phase 3: REM 수면
        LocalDateTime phase3Start = sleepStart.plusMinutes(phase1Duration + phase2Duration);
        stageResults.add(createAndValidateStage(
                SleepStage.REM, phase3Start, sleepEnd, 0.6, "Phase 3 (REM Sleep)", session));

        // 모든 단계가 성공적으로 생성되었는지 확인
        List<String> failures = stageResults.stream()
                .filter(result -> !result.isSuccess())
                .map(SleepStageCreationResult::getErrorMessage)
                .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            // 일부 단계 생성 실패 시 전체 작업 중단하고 로그 남김
            log.error("기본 수면 단계 생성 실패 - sessionId={}, 실패한 단계들: {}", 
                    session.getId(), String.join(", ", failures));
            log.warn("데이터 일관성을 위해 기본 수면 단계 저장을 중단합니다 - sessionId={}", session.getId());
            return;
        }

        // 모든 단계가 성공적으로 생성된 경우에만 일괄 저장 (원자성 보장)
        List<SleepStageData> validStages = stageResults.stream()
                .map(SleepStageCreationResult::getStageData)
                .collect(Collectors.toList());

        sleepStageDataRepository.saveAll(validStages);
        log.info("기본 수면 단계 생성 완료 - sessionId={}, 생성된 단계 수={}", 
                session.getId(), validStages.size());
    }

    /**
     * 수면 단계를 생성하고 검증하는 헬퍼 메서드
     */
    private SleepStageCreationResult createAndValidateStage(SleepStage sleepStage, 
                                                           LocalDateTime startTime, 
                                                           LocalDateTime endTime, 
                                                           double confidence, 
                                                           String phaseName,
                                                           SleepSession session) {
        try {
            SleepStageData stageData = SleepStageData.create(sleepStage, startTime, confidence);
            
            if (!stageData.tryEndStage(endTime)) {
                String errorMsg = String.format("%s 종료 실패: start=%s, end=%s", phaseName, startTime, endTime);
                return SleepStageCreationResult.failure(errorMsg);
            }
            
            stageData.assignToSleepSession(session);
            return SleepStageCreationResult.success(stageData);
            
        } catch (Exception e) {
            String errorMsg = String.format("%s 생성 중 예외 발생: %s", phaseName, e.getMessage());
            log.warn(errorMsg, e);
            return SleepStageCreationResult.failure(errorMsg);
        }
    }

    /**
     * 수면 단계 생성 결과를 담는 내부 클래스
     */
    private static class SleepStageCreationResult {
        private final boolean success;
        private final SleepStageData stageData;
        private final String errorMessage;

        private SleepStageCreationResult(boolean success, SleepStageData stageData, String errorMessage) {
            this.success = success;
            this.stageData = stageData;
            this.errorMessage = errorMessage;
        }

        public static SleepStageCreationResult success(SleepStageData stageData) {
            return new SleepStageCreationResult(true, stageData, null);
        }

        public static SleepStageCreationResult failure(String errorMessage) {
            return new SleepStageCreationResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public SleepStageData getStageData() {
            return stageData;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private Map<String, Long> calculateStageDistribution(List<SleepSession> sessions) {
        Map<String, Long> distribution = new HashMap<>();
        
        for (SleepSession session : sessions) {
            List<Object[]> stageData = sleepStageDataRepository.findStageDurationBySleepSession(session);
            for (Object[] data : stageData) {
                SleepStage stage = (SleepStage) data[0];
                Long duration = ((Number) data[1]).longValue();
                distribution.merge(stage.getDescription(), duration, Long::sum);
            }
        }
        
        return distribution;
    }

    private Double calculateAverageStagePercentage(List<SleepSession> sessions, SleepStage stage) {
        return sessions.stream()
                .mapToDouble(session -> {
                    Optional<Long> stageDuration = switch (stage) {
                        case DEEP_SLEEP -> sleepStageDataRepository.findTotalDeepSleepDurationBySleepSession(session);
                        case REM -> sleepStageDataRepository.findTotalREMSleepDurationBySleepSession(session);
                        case LIGHT_SLEEP -> sleepStageDataRepository.findTotalLightSleepDurationBySleepSession(session);
                        default -> Optional.of(0L);
                    };
                    
                    Long totalDuration = Optional.ofNullable(session.getActualSleepDurationMinutes()).orElse(0).longValue();
                    
                    if (totalDuration == 0) return 0.0;
                    
                    return stageDuration.orElse(0L).doubleValue() / totalDuration * 100;
                })
                .average()
                .orElse(0.0);
    }

    private SleepStatisticsResponse calculateStatisticsForSessions(List<SleepSession> completedSessions, int totalSessionsInPeriod) {
        if (completedSessions.isEmpty()) {
            return createEmptyStatistics();
        }

        Double avgSleepDuration = completedSessions.stream()
                .mapToInt(s -> s.getActualSleepDurationMinutes() != null ? s.getActualSleepDurationMinutes() : 0)
                .average()
                .orElse(0.0);

        Double avgSleepEfficiency = completedSessions.stream()
                .mapToDouble(s -> s.getSleepEfficiencyPercentage() != null ? s.getSleepEfficiencyPercentage() : 0.0)
                .average()
                .orElse(0.0);

        Map<String, Long> stageDistribution = calculateStageDistribution(completedSessions);
        
        Double avgDeepSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.DEEP_SLEEP);
        Double avgREMSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.REM);
        Double avgLightSleepPercentage = calculateAverageStagePercentage(completedSessions, SleepStage.LIGHT_SLEEP);

        Integer avgWakeUpCount = (int) completedSessions.stream()
                .mapToInt(s -> s.getWakeUpCount() != null ? s.getWakeUpCount() : 0)
                .average().orElse(0.0);

        Double avgMovementCount = completedSessions.stream()
                .mapToInt(s -> s.getTotalMovementCount() != null ? s.getTotalMovementCount() : 0)
                .average().orElse(0.0);

        return new SleepStatisticsResponse(
                avgSleepDuration,
                avgSleepEfficiency,
                totalSessionsInPeriod,  // 해당 기간의 전체 세션 수 (완료되지 않은 것 포함)
                completedSessions.size(),  // 완료된 세션 수
                stageDistribution,
                avgDeepSleepPercentage,
                avgREMSleepPercentage,
                avgLightSleepPercentage,
                avgWakeUpCount,
                avgMovementCount
        );
    }

    private SleepStatisticsResponse createEmptyStatistics() {
        return new SleepStatisticsResponse(
                0.0, 0.0, 0, 0,
                new HashMap<>(),
                0.0, 0.0, 0.0,
                0, 0.0
        );
    }
} 