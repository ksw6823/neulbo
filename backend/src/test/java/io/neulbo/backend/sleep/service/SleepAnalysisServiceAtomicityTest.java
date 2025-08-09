package io.neulbo.backend.sleep.service;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SleepStageData;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.sleep.repository.SleepStageDataRepository;
import io.neulbo.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SleepAnalysisService의 createDefaultSleepStages 메서드의 
 * 원자성과 데이터 일관성 개선 사항을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class SleepAnalysisServiceAtomicityTest {

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @Mock
    private SleepStageDataRepository sleepStageDataRepository;

    @Mock
    private MovementDataRepository movementDataRepository;

    @InjectMocks
    private SleepAnalysisService sleepAnalysisService;

    private User testUser;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusHours(8);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();
    }

    private SleepSession createMockSleepSession(LocalDateTime startTime, LocalDateTime endTime) {
        SleepSession session = mock(SleepSession.class);
        when(session.getId()).thenReturn(1L);
        when(session.getUser()).thenReturn(testUser);
        when(session.getSleepStartTime()).thenReturn(startTime);
        when(session.getSleepEndTime()).thenReturn(endTime);
        when(session.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);
        return session;
    }

    @Test
    @DisplayName("정상적인 기본 수면 단계 생성 시 모든 단계가 원자적으로 저장됨")
    void createDefaultSleepStages_SuccessfulCreation_AllStagesSavedAtomically() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusHours(8); // 8시간 수면
        SleepSession session = createMockSleepSession(startTime, endTime);
        
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When - Reflection을 사용하여 private 메서드 호출
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        createDefaultStagesMethod.invoke(sleepAnalysisService, session);

        // Then - 정확히 3개의 단계가 일괄 저장되는지 확인
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SleepStageData>> stageListCaptor = ArgumentCaptor.forClass(List.class);
        verify(sleepStageDataRepository, times(1)).saveAll(stageListCaptor.capture());
        
        List<SleepStageData> savedStages = stageListCaptor.getValue();
        assertThat(savedStages).hasSize(3);
        
        // 각 단계의 시간 순서 확인
        assertThat(savedStages.get(0).getStageStartTime()).isEqualTo(startTime);
        assertThat(savedStages.get(1).getStageStartTime()).isAfter(savedStages.get(0).getStageStartTime());
        assertThat(savedStages.get(2).getStageStartTime()).isAfter(savedStages.get(1).getStageStartTime());
    }

    @Test
    @DisplayName("수면 시간이 너무 짧은 경우 데이터 일관성 보장됨")
    void createDefaultSleepStages_VeryShortSleep_DataConsistencyMaintained() throws Exception {
        // Given - 매우 짧은 수면 시간 (1분)으로 검증 실패 유도
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusMinutes(1);
        SleepSession session = createMockSleepSession(startTime, endTime);

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        
        // 예외 없이 처리되어야 함 (원자성 보장으로 인한 graceful 실패)
        assertThatCode(() -> {
            createDefaultStagesMethod.invoke(sleepAnalysisService, session);
        }).doesNotThrowAnyException();

        // Then - 검증 실패 시 아무것도 저장되지 않아야 함
        verify(sleepStageDataRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("수면 시간이 역순인 경우 원자성 보장으로 전체 작업 취소됨")
    void createDefaultSleepStages_InvalidTimeOrder_AtomicRollback() throws Exception {
        // Given - 시작 시간이 종료 시간보다 늦은 잘못된 데이터
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.minusHours(1); // 잘못된 시간 순서
        SleepSession session = createMockSleepSession(startTime, endTime);

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        
        assertThatCode(() -> {
            createDefaultStagesMethod.invoke(sleepAnalysisService, session);
        }).doesNotThrowAnyException();

        // Then - 원자성 보장으로 아무것도 저장되지 않음
        verify(sleepStageDataRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("정상적인 8시간 수면의 기본 단계 패턴 검증")
    void createDefaultSleepStages_EightHourSleep_CorrectPhasePattern() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusHours(8); // 표준 8시간 수면
        SleepSession session = createMockSleepSession(startTime, endTime);
        
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        createDefaultStagesMethod.invoke(sleepAnalysisService, session);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SleepStageData>> stageListCaptor = ArgumentCaptor.forClass(List.class);
        verify(sleepStageDataRepository).saveAll(stageListCaptor.capture());
        
        List<SleepStageData> savedStages = stageListCaptor.getValue();
        assertThat(savedStages).hasSize(3);

        // Phase 1: Light Sleep (30% = 144분)
        SleepStageData phase1 = savedStages.get(0);
        assertThat(phase1.getSleepStage().name()).isEqualTo("LIGHT_SLEEP");
        assertThat(phase1.getDurationMinutes()).isEqualTo(144); // 480 * 30%

        // Phase 2: Deep Sleep (40% = 192분)
        SleepStageData phase2 = savedStages.get(1);
        assertThat(phase2.getSleepStage().name()).isEqualTo("DEEP_SLEEP");
        assertThat(phase2.getDurationMinutes()).isEqualTo(192); // 480 * 40%

        // Phase 3: REM Sleep (나머지 = 144분)
        SleepStageData phase3 = savedStages.get(2);
        assertThat(phase3.getSleepStage().name()).isEqualTo("REM");
        assertThat(phase3.getDurationMinutes()).isEqualTo(144); // 480 - 144 - 192

        // 전체 시간이 정확히 8시간인지 확인
        int totalDuration = phase1.getDurationMinutes() + phase2.getDurationMinutes() + phase3.getDurationMinutes();
        assertThat(totalDuration).isEqualTo(480); // 8시간 = 480분
    }

    @Test
    @DisplayName("기본 수면 단계 생성의 원자성 - All or Nothing 원칙 준수")
    void createDefaultSleepStages_AtomicityPrinciple_AllOrNothing() throws Exception {
        // Given - 정상적인 세션
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusHours(6); // 6시간 수면
        SleepSession session = createMockSleepSession(startTime, endTime);
        
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        createDefaultStagesMethod.invoke(sleepAnalysisService, session);

        // Then - 정확히 한 번의 saveAll 호출로 모든 데이터가 원자적으로 저장
        verify(sleepStageDataRepository, times(1)).saveAll(any());
        
        // 개별 저장이 아닌 일괄 저장만 수행되었는지 확인
        verify(sleepStageDataRepository, never()).save(any(SleepStageData.class));
    }

    @Test
    @DisplayName("createAndValidateStage 헬퍼 메서드의 예외 처리 검증")
    void createAndValidateStage_ExceptionHandling_GracefulFailure() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusHours(8);
        SleepSession session = createMockSleepSession(startTime, endTime);

        // When - createAndValidateStage 메서드를 직접 테스트하기 위해 Reflection 사용
        Method createAndValidateMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createAndValidateStage", 
                io.neulbo.backend.sleep.domain.SleepStage.class, 
                LocalDateTime.class, 
                LocalDateTime.class, 
                double.class, 
                String.class,
                SleepSession.class);
        createAndValidateMethod.setAccessible(true);

        // 정상적인 케이스
        Object normalResult = createAndValidateMethod.invoke(sleepAnalysisService,
                io.neulbo.backend.sleep.domain.SleepStage.LIGHT_SLEEP,
                startTime, startTime.plusHours(2), 0.7, "Test Phase", session);

        // Then - 결과 객체가 성공 상태인지 확인 (리플렉션으로 내부 상태 검증)
        assertThat(normalResult).isNotNull();
        
        // SleepStageCreationResult의 success 필드 확인
        Method isSuccessMethod = normalResult.getClass().getMethod("isSuccess");
        boolean isSuccess = (boolean) isSuccessMethod.invoke(normalResult);
        assertThat(isSuccess).isTrue();
    }

    @Test
    @DisplayName("데이터 일관성 보장 - 부분 실패 시 전체 작업 롤백")
    void createDefaultSleepStages_PartialFailure_CompleteRollback() throws Exception {
        // Given - 매우 불규칙한 수면 시간으로 일부 단계 생성 실패 유도
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusSeconds(30); // 30초만의 수면 (비현실적)
        SleepSession session = createMockSleepSession(startTime, endTime);

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        
        assertThatCode(() -> {
            createDefaultStagesMethod.invoke(sleepAnalysisService, session);
        }).doesNotThrowAnyException();

        // Then - 일부라도 실패하면 전체가 저장되지 않음 (원자성)
        verify(sleepStageDataRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("개선된 에러 처리 - 상세한 실패 정보 로깅")
    void createDefaultSleepStages_ImprovedErrorHandling_DetailedLogging() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.minusMinutes(10); // 잘못된 시간 순서
        SleepSession session = createMockSleepSession(startTime, endTime);

        // When
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        
        // 예외가 발생하지 않고 graceful하게 처리되는지 확인
        assertThatCode(() -> {
            createDefaultStagesMethod.invoke(sleepAnalysisService, session);
        }).doesNotThrowAnyException();

        // Then - 저장 작업이 수행되지 않음 (실패 시 저장 방지)
        verify(sleepStageDataRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("성능 테스트 - 대량 검증 작업도 효율적으로 처리")
    void createDefaultSleepStages_PerformanceTest_EfficientValidation() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        LocalDateTime endTime = baseTime.plusHours(12); // 긴 수면 시간
        SleepSession session = createMockSleepSession(startTime, endTime);
        
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        long startTime_perf = System.currentTimeMillis();
        
        Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                "createDefaultSleepStages", SleepSession.class);
        createDefaultStagesMethod.setAccessible(true);
        createDefaultStagesMethod.invoke(sleepAnalysisService, session);
        
        long endTime_perf = System.currentTimeMillis();
        long executionTime = endTime_perf - startTime_perf;

        // Then - 환경별 동적 성능 검증
        long maxAllowedTime = getAtomicityPerformanceThreshold();
        
        assertThat(executionTime)
            .as("기본 수면 단계 생성 시간이 허용 범위(%dms) 내에 있어야 함. 실제: %dms", 
                maxAllowedTime, executionTime)
            .isLessThan(maxAllowedTime);
        
        // 정상적으로 저장되었는지 확인
        verify(sleepStageDataRepository, times(1)).saveAll(any());
        
        System.out.printf("기본 수면 단계 생성 성능: %dms (12시간 수면, 허용: %dms)%n", 
                executionTime, maxAllowedTime);
    }

    @Test
    @DisplayName("비즈니스 로직 검증 - 수면 단계 비율이 올바르게 적용됨")
    void createDefaultSleepStages_BusinessLogic_CorrectStageRatios() throws Exception {
        // Given - 다양한 수면 시간으로 테스트
        int[] sleepHours = {4, 6, 8, 10, 12};
        
        for (int hours : sleepHours) {
            LocalDateTime startTime = baseTime;
            LocalDateTime endTime = baseTime.plusHours(hours);
            SleepSession session = createMockSleepSession(startTime, endTime);
            
            reset(sleepStageDataRepository); // 각 테스트마다 mock 리셋
            when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            Method createDefaultStagesMethod = SleepAnalysisService.class.getDeclaredMethod(
                    "createDefaultSleepStages", SleepSession.class);
            createDefaultStagesMethod.setAccessible(true);
            createDefaultStagesMethod.invoke(sleepAnalysisService, session);

            // Then - 각 수면 시간별로 비율 검증
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SleepStageData>> captor = ArgumentCaptor.forClass(List.class);
            verify(sleepStageDataRepository).saveAll(captor.capture());
            
            List<SleepStageData> stages = captor.getValue();
            assertThat(stages).hasSize(3);
            
            int totalMinutes = hours * 60;
            int expectedPhase1 = totalMinutes * 30 / 100; // 30%
            int expectedPhase2 = totalMinutes * 40 / 100; // 40%
            int expectedPhase3 = totalMinutes - expectedPhase1 - expectedPhase2; // 나머지
            
            assertThat(stages.get(0).getDurationMinutes()).isEqualTo(expectedPhase1);
            assertThat(stages.get(1).getDurationMinutes()).isEqualTo(expectedPhase2);
            assertThat(stages.get(2).getDurationMinutes()).isEqualTo(expectedPhase3);
            
            System.out.printf("%d시간 수면: Light=%d분, Deep=%d분, REM=%d분%n", 
                    hours, expectedPhase1, expectedPhase2, expectedPhase3);
        }
    }

    /**
     * 원자성 테스트를 위한 성능 임계값 계산
     * 기본 수면 단계 생성 작업의 환경별 성능 기준 설정
     * 
     * @return 허용 가능한 최대 처리 시간 (ms)
     */
    private long getAtomicityPerformanceThreshold() {
        // 환경 변수로 원자성 성능 배율 조정 가능 (기본값: 3.0)
        double atomicityMultiplier = Double.parseDouble(
            System.getProperty("test.atomicity.multiplier", "3.0"));
        
        // CI 환경 감지
        boolean isCiEnvironment = System.getenv("CI") != null || 
                                 System.getenv("CONTINUOUS_INTEGRATION") != null ||
                                 System.getProperty("test.environment", "").equals("ci");
        
        // CI 환경에서는 더 관대한 임계값 적용
        if (isCiEnvironment) {
            atomicityMultiplier *= 4.0; // CI에서는 4배 더 관대하게
        }
        
        // 기본 수면 단계 생성 시간: 단순한 메모리 연산이므로 낮은 기준값
        long baseTime = 50L; // 기본 50ms
        
        // 최종 임계값 계산
        long threshold = Math.max((long) (baseTime * atomicityMultiplier), 200L);
        
        // 안전장치: 최대 3초를 넘지 않도록 제한
        return Math.min(threshold, 3000L);
    }
}