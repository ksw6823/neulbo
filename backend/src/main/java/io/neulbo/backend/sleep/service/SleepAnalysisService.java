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
            // 기존 수면 단계 데이터 삭제 (재분석인 경우)
            List<SleepStageData> existingStages = sleepStageDataRepository.findBySleepSessionOrderByStageStartTimeAsc(session);
            if (!existingStages.isEmpty()) {
                sleepStageDataRepository.deleteAll(existingStages);
                log.info("기존 수면 단계 데이터 삭제: {} 개", existingStages.size());
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

        return new SleepStatisticsResponse(
                avgSleepDuration,
                avgSleepEfficiency,
                completedSessions.size(),
                completedSessions.size(),
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
        List<SleepSession> sessions = sleepSessionRepository.findByUserAndDateRange(user, startDate, endDate)
                .stream()
                .filter(SleepSession::isCompleted)
                .collect(Collectors.toList());

        return calculateStatisticsForSessions(sessions);
    }

    /**
     * 움직임 기반 수면 단계 분석 (간단한 규칙 기반)
     */
    private void analyzeAndCreateSleepStages(SleepSession session, List<MovementData> movementData) {
        LocalDateTime sleepStart = session.getSleepStartTime();
        LocalDateTime sleepEnd = session.getSleepEndTime();
        
        // 10분 단위로 분석
        long totalMinutes = ChronoUnit.MINUTES.between(sleepStart, sleepEnd);
        int intervalMinutes = 10;
        
        List<SleepStageData> stageDataList = new ArrayList<>();
        
        for (int i = 0; i < totalMinutes; i += intervalMinutes) {
            LocalDateTime intervalStart = sleepStart.plusMinutes(i);
            LocalDateTime intervalEnd = sleepStart.plusMinutes(Math.min(i + intervalMinutes, totalMinutes));
            
            // 해당 구간의 움직임 데이터 조회
            List<MovementData> intervalMovements = movementData.stream()
                    .filter(m -> !m.getTimestamp().isBefore(intervalStart) && m.getTimestamp().isBefore(intervalEnd))
                    .collect(Collectors.toList());
            
            // 수면 단계 결정
            SleepStage stage = determineSleepStage(intervalMovements, intervalStart, sleepStart);
            double confidence = calculateConfidence(intervalMovements);
            
            // 수면 단계 데이터 생성
            SleepStageData stageData = SleepStageData.create(stage, intervalStart, confidence);
            stageData.endStage(intervalEnd);
            stageData.setSleepSession(session);
            
            // 움직임 카운트 설정
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
     */
    private void createDefaultSleepStages(SleepSession session) {
        LocalDateTime sleepStart = session.getSleepStartTime();
        LocalDateTime sleepEnd = session.getSleepEndTime();
        long totalMinutes = ChronoUnit.MINUTES.between(sleepStart, sleepEnd);

        List<SleepStageData> defaultStages = new ArrayList<>();

        // 전체 수면을 3단계로 나눔: 얕은잠 -> 깊은잠 -> 얕은잠
        long phase1Duration = totalMinutes * 30 / 100; // 30%
        long phase2Duration = totalMinutes * 40 / 100; // 40%
        long phase3Duration = totalMinutes - phase1Duration - phase2Duration; // 나머지

        // Phase 1: 얕은 잠
        SleepStageData stage1 = SleepStageData.create(SleepStage.LIGHT_SLEEP, sleepStart, 0.7);
        stage1.endStage(sleepStart.plusMinutes(phase1Duration));
        stage1.setSleepSession(session);

        // Phase 2: 깊은 잠
        SleepStageData stage2 = SleepStageData.create(SleepStage.DEEP_SLEEP, sleepStart.plusMinutes(phase1Duration), 0.8);
        stage2.endStage(sleepStart.plusMinutes(phase1Duration + phase2Duration));
        stage2.setSleepSession(session);

        // Phase 3: 얕은 잠 + REM
        SleepStageData stage3 = SleepStageData.create(SleepStage.REM, sleepStart.plusMinutes(phase1Duration + phase2Duration), 0.6);
        stage3.endStage(sleepEnd);
        stage3.setSleepSession(session);

        defaultStages.addAll(Arrays.asList(stage1, stage2, stage3));
        sleepStageDataRepository.saveAll(defaultStages);
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
                    
                    Long totalDuration = Optional.ofNullable(session.getActualSleepDurationMinutes()).orElse(0);
                    
                    if (totalDuration == 0) return 0.0;
                    
                    return stageDuration.orElse(0L).doubleValue() / totalDuration * 100;
                })
                .average()
                .orElse(0.0);
    }

    private SleepStatisticsResponse calculateStatisticsForSessions(List<SleepSession> sessions) {
        if (sessions.isEmpty()) {
            return createEmptyStatistics();
        }

        Double avgSleepDuration = sessions.stream()
                .mapToInt(s -> s.getActualSleepDurationMinutes() != null ? s.getActualSleepDurationMinutes() : 0)
                .average()
                .orElse(0.0);

        Double avgSleepEfficiency = sessions.stream()
                .mapToDouble(s -> s.getSleepEfficiencyPercentage() != null ? s.getSleepEfficiencyPercentage() : 0.0)
                .average()
                .orElse(0.0);

        Map<String, Long> stageDistribution = calculateStageDistribution(sessions);
        
        Double avgDeepSleepPercentage = calculateAverageStagePercentage(sessions, SleepStage.DEEP_SLEEP);
        Double avgREMSleepPercentage = calculateAverageStagePercentage(sessions, SleepStage.REM);
        Double avgLightSleepPercentage = calculateAverageStagePercentage(sessions, SleepStage.LIGHT_SLEEP);

        Integer avgWakeUpCount = (int) sessions.stream()
                .mapToInt(s -> s.getWakeUpCount() != null ? s.getWakeUpCount() : 0)
                .average().orElse(0.0);

        Double avgMovementCount = sessions.stream()
                .mapToInt(s -> s.getTotalMovementCount() != null ? s.getTotalMovementCount() : 0)
                .average().orElse(0.0);

        return new SleepStatisticsResponse(
                avgSleepDuration,
                avgSleepEfficiency,
                sessions.size(),
                sessions.size(),
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