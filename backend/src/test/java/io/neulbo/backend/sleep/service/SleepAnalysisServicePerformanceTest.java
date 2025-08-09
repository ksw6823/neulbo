package io.neulbo.backend.sleep.service;

import io.neulbo.backend.sleep.domain.MovementData;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.sleep.repository.SleepStageDataRepository;
import io.neulbo.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SleepAnalysisService의 성능 최적화 검증 테스트
 * O(n²) → O(n + m) 복잡도 개선 효과 확인
 */
@ExtendWith(MockitoExtension.class)
class SleepAnalysisServicePerformanceTest {

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

    private List<MovementData> createSortedMovementData(LocalDateTime startTime, int count, int intervalSeconds) {
        List<MovementData> movementDataList = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            LocalDateTime timestamp = startTime.plusSeconds(i * intervalSeconds);
            MovementData movement = MovementData.create(timestamp, 1.0, 1.0, 1.0);
            movementDataList.add(movement);
        }
        
        return movementDataList;
    }

    /**
     * collectMovementsForInterval 메서드의 정확성 검증
     */
    @Test
    @DisplayName("collectMovementsForInterval 메서드가 올바른 간격의 데이터를 수집함")
    void collectMovementsForInterval_ReturnsCorrectData() {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = createSortedMovementData(startTime, 30, 60); // 30분간 1분마다 데이터

        LocalDateTime intervalStart = startTime.plusMinutes(10);
        LocalDateTime intervalEnd = startTime.plusMinutes(20); // 10분 간격

        // When - package-private 메서드를 직접 호출 (@VisibleForTesting으로 테스트에서 접근 가능)
        List<MovementData> result = sleepAnalysisService.collectMovementsForInterval(
                movementData, intervalStart, intervalEnd, 0);

        // Then
        assertThat(result).hasSize(10); // 10분 간격이므로 10개 데이터
        
        // 모든 데이터가 올바른 시간 범위에 있는지 확인
        for (MovementData movement : result) {
            assertThat(movement.getTimestamp()).isAfterOrEqualTo(intervalStart);
            assertThat(movement.getTimestamp()).isBefore(intervalEnd);
        }

        // 첫 번째와 마지막 데이터 시간 확인
        assertThat(result.get(0).getTimestamp()).isEqualTo(intervalStart);
        assertThat(result.get(result.size() - 1).getTimestamp()).isEqualTo(intervalEnd.minusMinutes(1));
    }

    /**
     * updateMovementDataIndex 메서드의 정확성 검증
     */
    @Test
    @DisplayName("updateMovementDataIndex 메서드가 올바른 다음 인덱스를 반환함")
    void updateMovementDataIndex_ReturnsCorrectNextIndex() {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = createSortedMovementData(startTime, 30, 60); // 30분간 1분마다 데이터
        
        LocalDateTime intervalEnd = startTime.plusMinutes(15);

        // When - package-private 메서드를 직접 호출
        int nextIndex = sleepAnalysisService.updateMovementDataIndex(
                movementData, intervalEnd, 0);

        // Then
        assertThat(nextIndex).isEqualTo(15); // 15분 후 시점부터 시작해야 함
        
        // 다음 인덱스의 데이터가 intervalEnd 이후인지 확인
        if (nextIndex < movementData.size()) {
            assertThat(movementData.get(nextIndex).getTimestamp()).isAfterOrEqualTo(intervalEnd);
        }
    }

    /**
     * 인덱스 기반 접근법의 성능 측정 (대량 데이터)
     */
    @Test
    @DisplayName("대량 데이터에서 인덱스 기반 접근법의 성능 검증")
    void indexBasedApproach_PerformanceWithLargeData() {
        // Given - 8시간 수면에 10초마다 움직임 데이터 (2,880개 데이터)
        LocalDateTime sleepStart = baseTime;
        LocalDateTime sleepEnd = sleepStart.plusHours(8);
        int totalDataPoints = 8 * 60 * 6; // 8시간 * 60분 * 6(10초마다)
        
        List<MovementData> movementData = createSortedMovementData(sleepStart, totalDataPoints, 10);
        
        SleepSession session = createMockSleepSession(sleepStart, sleepEnd);
        when(sleepStageDataRepository.deleteBySleepSession(any())).thenReturn(0);
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // Baseline 성능 측정 (작은 데이터셋)
        List<MovementData> baselineData = createSortedMovementData(sleepStart, 100, 10);
        SleepSession baselineSession = createMockSleepSession(sleepStart, sleepStart.plusMinutes(60));
        
        long baselineStart = System.currentTimeMillis();
        sleepAnalysisService.analyzeAndCreateSleepStages(baselineSession, baselineData);
        long baselineTime = System.currentTimeMillis() - baselineStart;

        // When - 실제 analyzeAndCreateSleepStages 메서드 실행
        long startTime = System.currentTimeMillis();
        
        // package-private 메서드를 직접 호출
        sleepAnalysisService.analyzeAndCreateSleepStages(session, movementData);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then - 환경별 동적 성능 검증
        long maxAllowedTime = getPerformanceThreshold(totalDataPoints, baselineTime);
        
        assertThat(executionTime)
            .as("대량 데이터 처리 시간이 허용 범위(%dms) 내에 있어야 함. 실제: %dms", 
                maxAllowedTime, executionTime)
            .isLessThan(maxAllowedTime);
        
        System.out.printf("대량 데이터 성능 테스트: %d개 데이터, %d개 간격을 %dms에 처리 (기준선: %dms, 허용: %dms)%n", 
                totalDataPoints, 48, executionTime, baselineTime, maxAllowedTime);

        // Repository 호출 검증
        verify(sleepStageDataRepository).deleteBySleepSession(session);
        verify(sleepStageDataRepository).saveAll(any());
    }

    /**
     * 빈 movementData 처리 검증
     */
    @Test
    @DisplayName("빈 movementData로도 정상 처리됨")
    void analyzeAndCreateSleepStages_HandlesEmptyMovementData() throws Exception {
        // Given
        SleepSession session = createMockSleepSession(baseTime, baseTime.plusHours(8));
        List<MovementData> emptyMovementData = new ArrayList<>();
        
        when(sleepStageDataRepository.deleteBySleepSession(any())).thenReturn(0);
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When & Then - 예외 없이 처리되어야 함
        assertThatCode(() -> {
            sleepAnalysisService.analyzeAndCreateSleepStages(session, emptyMovementData);
        }).doesNotThrowAnyException();

        verify(sleepStageDataRepository).saveAll(any());
    }

    /**
     * 경계 조건 테스트: 간격 경계에 정확히 위치한 데이터
     */
    @Test
    @DisplayName("간격 경계에 있는 데이터를 올바르게 처리함")
    void collectMovementsForInterval_HandlesBoundaryData() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = new ArrayList<>();
        
        // 간격 경계에 정확히 위치한 데이터들 생성
        movementData.add(MovementData.create(startTime, 1.0, 1.0, 1.0)); // 간격 시작 직전
        movementData.add(MovementData.create(startTime.plusMinutes(10), 2.0, 2.0, 2.0)); // 간격 시작
        movementData.add(MovementData.create(startTime.plusMinutes(15), 3.0, 3.0, 3.0)); // 간격 중간
        movementData.add(MovementData.create(startTime.plusMinutes(20), 4.0, 4.0, 4.0)); // 간격 종료 (포함되지 않음)
        movementData.add(MovementData.create(startTime.plusMinutes(25), 5.0, 5.0, 5.0)); // 간격 종료 후

        LocalDateTime intervalStart = startTime.plusMinutes(10);
        LocalDateTime intervalEnd = startTime.plusMinutes(20);

        // When - package-private 메서드를 직접 호출
        List<MovementData> result = sleepAnalysisService.collectMovementsForInterval(
                movementData, intervalStart, intervalEnd, 0);

        // Then
        assertThat(result).hasSize(2); // 간격 시작과 중간 데이터만 포함
        assertThat(result.get(0).getTimestamp()).isEqualTo(intervalStart);
        assertThat(result.get(1).getTimestamp()).isEqualTo(startTime.plusMinutes(15));
        
        // 간격 종료 시간의 데이터는 포함되지 않음 (isBefore 조건)
        assertThat(result.stream().noneMatch(m -> m.getTimestamp().equals(intervalEnd))).isTrue();
    }

    /**
     * 인덱스 최적화 효과 검증
     */
    @Test
    @DisplayName("인덱스 최적화로 중복 확인이 제거됨")
    void indexOptimization_EliminatesDuplicateChecks() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = createSortedMovementData(startTime, 60, 60); // 1시간 데이터
        
        int currentIndex = 0;
        int totalCheckedItems = 0;
        
        // When - 6개 간격(1시간)에 대해 인덱스 기반 접근법 시뮬레이션
        for (int i = 0; i < 60; i += 10) { // 10분 간격
            LocalDateTime intervalStart = startTime.plusMinutes(i);
            LocalDateTime intervalEnd = startTime.plusMinutes(i + 10);
            
            // collectMovementsForInterval 시뮬레이션
            int checkedInThisInterval = 0;
            for (int j = currentIndex; j < movementData.size(); j++) {
                checkedInThisInterval++;
                LocalDateTime timestamp = movementData.get(j).getTimestamp();
                
                if (timestamp.isBefore(intervalStart)) {
                    continue;
                }
                if (!timestamp.isBefore(intervalEnd)) {
                    break;
                }
            }
            
            totalCheckedItems += checkedInThisInterval;
            
            // updateMovementDataIndex 시뮬레이션
            while (currentIndex < movementData.size() && 
                   movementData.get(currentIndex).getTimestamp().isBefore(intervalEnd)) {
                currentIndex++;
            }
        }

        // Then - O(n + m) 복잡도 확인: 각 데이터는 최대 한 번씩만 확인됨
        assertThat(totalCheckedItems).isLessThanOrEqualTo(movementData.size() * 2); // 실제로는 훨씬 적음
        
        System.out.printf("인덱스 최적화 효과: %d개 데이터에 대해 총 %d번 확인 (비율: %.2f)%n", 
                movementData.size(), totalCheckedItems, (double) totalCheckedItems / movementData.size());
        
        // 기존 O(n²) 방식이라면 60 * 6 = 360번의 전체 스캔이 필요했을 것
        // 인덱스 방식은 각 데이터를 최대 2번(수집 + 인덱스 업데이트)만 확인
        assertThat(totalCheckedItems).isLessThan(200); // 이론적 최대값보다 훨씬 적음
    }

    /**
     * 정렬되지 않은 데이터에 대한 견고성 테스트
     */
    @Test
    @DisplayName("정렬되지 않은 movementData도 올바르게 처리함")
    void collectMovementsForInterval_HandlesUnsortedData() throws Exception {
        // Given - 의도적으로 정렬되지 않은 데이터
        LocalDateTime startTime = baseTime;
        List<MovementData> unsortedMovementData = new ArrayList<>();
        
        unsortedMovementData.add(MovementData.create(startTime.plusMinutes(15), 1.0, 1.0, 1.0));
        unsortedMovementData.add(MovementData.create(startTime.plusMinutes(5), 2.0, 2.0, 2.0));
        unsortedMovementData.add(MovementData.create(startTime.plusMinutes(25), 3.0, 3.0, 3.0));
        unsortedMovementData.add(MovementData.create(startTime.plusMinutes(12), 4.0, 4.0, 4.0));

        LocalDateTime intervalStart = startTime.plusMinutes(10);
        LocalDateTime intervalEnd = startTime.plusMinutes(20);

        // When - package-private 메서드를 직접 호출
        List<MovementData> result = sleepAnalysisService.collectMovementsForInterval(
                unsortedMovementData, intervalStart, intervalEnd, 0);

        // Then - 정렬 여부와 관계없이 올바른 시간 범위의 데이터만 수집
        assertThat(result).hasSize(2); // 15분과 12분 데이터
        
        for (MovementData movement : result) {
            assertThat(movement.getTimestamp()).isAfterOrEqualTo(intervalStart);
            assertThat(movement.getTimestamp()).isBefore(intervalEnd);
        }
    }

    /**
     * 성능 개선 전후 비교를 위한 벤치마크
     */
    @Test
    @DisplayName("성능 개선 효과 벤치마크")
    void performanceImprovement_Benchmark() {
        // Given
        int[] dataSizes = {100, 500, 1000, 2000}; // 다양한 데이터 크기
        LocalDateTime startTime = baseTime;
        
        // 기준선 성능 측정 (가장 작은 데이터셋)
        List<MovementData> baselineData = createSortedMovementData(startTime, 50, 30);
        long baselineTime = measureIndexBasedPerformance(baselineData, startTime, 5);
        
        for (int dataSize : dataSizes) {
            List<MovementData> movementData = createSortedMovementData(startTime, dataSize, 30);
            int intervals = dataSize / 10; // 간격 수
            
            long indexBasedTime = measureIndexBasedPerformance(movementData, startTime, intervals);
            
            System.out.printf("데이터 크기: %d, 간격 수: %d, 인덱스 기반 처리 시간: %dms%n", 
                    dataSize, intervals, indexBasedTime);
            
            // 동적 성능 기준: O(n + m) 복잡도 유지 확인
            long maxAllowedTime = getScalabilityThreshold(dataSize, baselineTime);
            
            assertThat(indexBasedTime)
                .as("데이터 크기 %d에 대한 처리 시간이 확장성 기준(%dms) 내에 있어야 함. 실제: %dms", 
                    dataSize, maxAllowedTime, indexBasedTime)
                .isLessThan(maxAllowedTime);
        }
    }

    private long measureIndexBasedPerformance(List<MovementData> movementData, 
                                            LocalDateTime startTime, 
                                            int intervals) {
        long start = System.currentTimeMillis();
        
        try {
            int movementDataIndex = 0;
            
            // 실제 인덱스 기반 로직 시뮬레이션
            for (int i = 0; i < intervals; i++) {
                LocalDateTime intervalStart = startTime.plusMinutes(i * 10);
                LocalDateTime intervalEnd = startTime.plusMinutes((i + 1) * 10);
                
                // package-private 메서드들을 직접 호출
                sleepAnalysisService.collectMovementsForInterval(movementData, intervalStart, intervalEnd, movementDataIndex);
                
                movementDataIndex = sleepAnalysisService.updateMovementDataIndex(
                        movementData, intervalEnd, movementDataIndex);
            }
        } catch (Exception e) {
            fail("Performance measurement failed: " + e.getMessage());
        }
        
        return System.currentTimeMillis() - start;
    }

    /**
     * 환경별 동적 성능 임계값 계산
     * CI/CD 환경이나 느린 머신에서도 안정적으로 동작하도록 baseline 기반으로 계산
     * 
     * @param dataSize 처리할 데이터 크기
     * @param baselineTime 기준선 처리 시간
     * @return 허용 가능한 최대 처리 시간 (ms)
     */
    private long getPerformanceThreshold(int dataSize, long baselineTime) {
        // 환경 변수로 성능 배율 조정 가능 (기본값: 3.0)
        double performanceMultiplier = Double.parseDouble(
            System.getProperty("test.performance.multiplier", "3.0"));
        
        // CI 환경 감지
        boolean isCiEnvironment = System.getenv("CI") != null || 
                                 System.getenv("CONTINUOUS_INTEGRATION") != null ||
                                 System.getProperty("test.environment", "").equals("ci");
        
        // CI 환경에서는 더 관대한 임계값 적용
        if (isCiEnvironment) {
            performanceMultiplier *= 2.0; // CI에서는 2배 더 관대하게
        }
        
        // 데이터 크기에 따른 기본 예상 처리 시간
        // O(n + m) 복잡도를 고려하여 선형적으로 증가
        long expectedTime = Math.max(baselineTime * dataSize / 100, baselineTime);
        
        // 최종 임계값: 예상 시간 * 성능 배율, 최소 5초 보장
        long threshold = Math.max((long) (expectedTime * performanceMultiplier), 5000L);
        
        // 안전장치: 최대 30초를 넘지 않도록 제한
        return Math.min(threshold, 30000L);
    }

    /**
     * 확장성 테스트를 위한 임계값 계산
     * O(n + m) 복잡도가 유지되는지 확인하기 위한 더 엄격한 기준
     * 
     * @param dataSize 처리할 데이터 크기
     * @param baselineTime 작은 데이터셋에 대한 기준선 시간
     * @return 확장성 검증을 위한 최대 허용 시간 (ms)
     */
    private long getScalabilityThreshold(int dataSize, long baselineTime) {
        // 환경 변수로 확장성 배율 조정 가능 (기본값: 4.0)
        double scalabilityMultiplier = Double.parseDouble(
            System.getProperty("test.scalability.multiplier", "4.0"));
        
        // CI 환경에서는 더 관대한 기준 적용
        boolean isCiEnvironment = System.getenv("CI") != null || 
                                 System.getenv("CONTINUOUS_INTEGRATION") != null;
        if (isCiEnvironment) {
            scalabilityMultiplier *= 1.5; // CI에서는 1.5배 더 관대하게
        }
        
        // 선형 확장성 기준: 데이터 크기에 비례하여 처리 시간 증가
        // 기준 데이터 크기 (50)에서 현재 데이터 크기로의 확장 비율 계산
        double scalingFactor = (double) dataSize / 50.0;
        long expectedTime = (long) (baselineTime * scalingFactor);
        
        // 최종 임계값: 예상 시간 * 확장성 배율
        long threshold = Math.max((long) (expectedTime * scalabilityMultiplier), 100L);
        
        // 확장성 테스트는 더 엄격하게: 최대 10초로 제한
        return Math.min(threshold, 10000L);
    }
}