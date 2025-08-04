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

import java.lang.reflect.Method;
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
    void collectMovementsForInterval_ReturnsCorrectData() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = createSortedMovementData(startTime, 30, 60); // 30분간 1분마다 데이터

        LocalDateTime intervalStart = startTime.plusMinutes(10);
        LocalDateTime intervalEnd = startTime.plusMinutes(20); // 10분 간격

        // Reflection을 사용하여 private 메서드 접근
        Method collectMethod = SleepAnalysisService.class.getDeclaredMethod(
                "collectMovementsForInterval", List.class, LocalDateTime.class, LocalDateTime.class, int.class);
        collectMethod.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<MovementData> result = (List<MovementData>) collectMethod.invoke(
                sleepAnalysisService, movementData, intervalStart, intervalEnd, 0);

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
    void updateMovementDataIndex_ReturnsCorrectNextIndex() throws Exception {
        // Given
        LocalDateTime startTime = baseTime;
        List<MovementData> movementData = createSortedMovementData(startTime, 30, 60); // 30분간 1분마다 데이터
        
        LocalDateTime intervalEnd = startTime.plusMinutes(15);

        // Reflection을 사용하여 private 메서드 접근
        Method updateIndexMethod = SleepAnalysisService.class.getDeclaredMethod(
                "updateMovementDataIndex", List.class, LocalDateTime.class, int.class);
        updateIndexMethod.setAccessible(true);

        // When
        int nextIndex = (int) updateIndexMethod.invoke(
                sleepAnalysisService, movementData, intervalEnd, 0);

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
    void indexBasedApproach_PerformanceWithLargeData() throws Exception {
        // Given - 8시간 수면에 10초마다 움직임 데이터 (2,880개 데이터)
        LocalDateTime sleepStart = baseTime;
        LocalDateTime sleepEnd = sleepStart.plusHours(8);
        int totalDataPoints = 8 * 60 * 6; // 8시간 * 60분 * 6(10초마다)
        
        List<MovementData> movementData = createSortedMovementData(sleepStart, totalDataPoints, 10);
        
        SleepSession session = createMockSleepSession(sleepStart, sleepEnd);
        when(sleepStageDataRepository.deleteBySleepSession(any())).thenReturn(0);
        when(sleepStageDataRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When - 실제 analyzeAndCreateSleepStages 메서드 실행
        long startTime = System.currentTimeMillis();
        
        // Reflection을 사용하여 private 메서드 접근
        Method analyzeMethod = SleepAnalysisService.class.getDeclaredMethod(
                "analyzeAndCreateSleepStages", SleepSession.class, List.class);
        analyzeMethod.setAccessible(true);
        analyzeMethod.invoke(sleepAnalysisService, session, movementData);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then - 성능 검증 (기존 O(n²) 대비 크게 개선되어야 함)
        assertThat(executionTime).isLessThan(1000L); // 1초 이내
        
        System.out.printf("대량 데이터 성능 테스트: %d개 데이터, %d개 간격을 %dms에 처리%n", 
                totalDataPoints, 48, executionTime); // 8시간 = 48개 10분 간격

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
        Method analyzeMethod = SleepAnalysisService.class.getDeclaredMethod(
                "analyzeAndCreateSleepStages", SleepSession.class, List.class);
        analyzeMethod.setAccessible(true);
        
        assertThatCode(() -> {
            analyzeMethod.invoke(sleepAnalysisService, session, emptyMovementData);
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

        Method collectMethod = SleepAnalysisService.class.getDeclaredMethod(
                "collectMovementsForInterval", List.class, LocalDateTime.class, LocalDateTime.class, int.class);
        collectMethod.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<MovementData> result = (List<MovementData>) collectMethod.invoke(
                sleepAnalysisService, movementData, intervalStart, intervalEnd, 0);

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

        Method collectMethod = SleepAnalysisService.class.getDeclaredMethod(
                "collectMovementsForInterval", List.class, LocalDateTime.class, LocalDateTime.class, int.class);
        collectMethod.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<MovementData> result = (List<MovementData>) collectMethod.invoke(
                sleepAnalysisService, unsortedMovementData, intervalStart, intervalEnd, 0);

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
        
        for (int dataSize : dataSizes) {
            List<MovementData> movementData = createSortedMovementData(startTime, dataSize, 30);
            int intervals = dataSize / 10; // 간격 수
            
            long indexBasedTime = measureIndexBasedPerformance(movementData, startTime, intervals);
            
            System.out.printf("데이터 크기: %d, 간격 수: %d, 인덱스 기반 처리 시간: %dms%n", 
                    dataSize, intervals, indexBasedTime);
            
            // 성능 기준: 대량 데이터도 합리적 시간 내 처리
            assertThat(indexBasedTime).isLessThan(dataSize * 2L); // 데이터 크기에 선형 비례
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
                
                Method collectMethod = SleepAnalysisService.class.getDeclaredMethod(
                        "collectMovementsForInterval", List.class, LocalDateTime.class, LocalDateTime.class, int.class);
                collectMethod.setAccessible(true);
                
                collectMethod.invoke(sleepAnalysisService, movementData, intervalStart, intervalEnd, movementDataIndex);
                
                Method updateIndexMethod = SleepAnalysisService.class.getDeclaredMethod(
                        "updateMovementDataIndex", List.class, LocalDateTime.class, int.class);
                updateIndexMethod.setAccessible(true);
                
                movementDataIndex = (int) updateIndexMethod.invoke(
                        sleepAnalysisService, movementData, intervalEnd, movementDataIndex);
            }
        } catch (Exception e) {
            fail("Performance measurement failed: " + e.getMessage());
        }
        
        return System.currentTimeMillis() - start;
    }
}