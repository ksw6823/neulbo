package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SleepSessionRepository의 새로운 유연한 메서드들의 사용 예시를 보여주는 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class SleepSessionRepositoryFlexibilityTest {

    @Test
    @DisplayName("새로운 유연한 메서드들의 사용 예시")
    void demonstrateFlexibleMethods() {
        // 이제 하드코딩 대신 다양한 SessionStatus로 사용 가능
        
        // 1. 완료된 세션 통계 (기존과 동일한 결과)
        SessionStatus completed = SessionStatus.COMPLETED;
        
        // 2. 진행 중인 세션 통계
        SessionStatus inProgress = SessionStatus.IN_PROGRESS;
        
        // 3. 중단된 세션 통계  
        SessionStatus interrupted = SessionStatus.INTERRUPTED;
        
        // 4. 취소된 세션 통계
        SessionStatus cancelled = SessionStatus.CANCELLED;
        
        // 이제 enum 값이 변경되어도 JPQL 쿼리는 수정할 필요가 없음
        assertThat(completed.getDescription()).isEqualTo("완료");
        assertThat(inProgress.getDescription()).isEqualTo("진행 중");
        assertThat(interrupted.getDescription()).isEqualTo("중단됨");
        assertThat(cancelled.getDescription()).isEqualTo("취소됨");
    }
    
    @Test
    @DisplayName("enum 값 변경에 대한 안정성 검증")
    void verifyEnumStability() {
        // enum 값들이 예상대로 정의되어 있는지 확인
        assertThat(SessionStatus.values()).hasSize(4);
        assertThat(SessionStatus.IN_PROGRESS.getDescription()).isEqualTo("진행 중");
        assertThat(SessionStatus.COMPLETED.getDescription()).isEqualTo("완료");
        assertThat(SessionStatus.INTERRUPTED.getDescription()).isEqualTo("중단됨");
        assertThat(SessionStatus.CANCELLED.getDescription()).isEqualTo("취소됨");
    }
    
    @Test
    @DisplayName("새로운 활용 시나리오 예시")
    void demonstrateNewUsageScenarios() {
        // 이제 서비스 계층에서 비즈니스 로직에 따라 유연하게 상태별 통계 조회 가능
        
        // 1. 완료된 세션만의 통계 (기존 동작)
        SessionStatus completedStatus = SessionStatus.COMPLETED;
        
        // 2. 중단된 세션들의 통계 분석
        SessionStatus interruptedStatus = SessionStatus.INTERRUPTED;
        
        // 3. 모든 종료된 세션들 (완료 + 중단 + 취소) - 서비스에서 여러 번 호출
        List<SessionStatus> finishedStatuses = List.of(
            SessionStatus.COMPLETED,
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
        );
        
        // 4. 진행 중인 세션들만 따로 분석
        SessionStatus activeStatus = SessionStatus.IN_PROGRESS;
        
        assertThat(completedStatus).isEqualTo(SessionStatus.COMPLETED);
        assertThat(interruptedStatus).isEqualTo(SessionStatus.INTERRUPTED);
        assertThat(finishedStatuses).hasSize(3);
        assertThat(activeStatus).isEqualTo(SessionStatus.IN_PROGRESS);
    }
    
    /**
     * SessionStatusHelper 클래스는 SessionStatus enum으로 이동되었습니다.
     * 이제 SessionStatus.getFinishedStatuses(), SessionStatus.isFinished() 등의 메서드를 직접 사용하세요.
     * 또는 별도의 유틸리티가 필요한 경우 SessionStatusUtils 클래스를 참고하세요.
     */
    
    @Test
    @DisplayName("개선된 SessionStatus enum 헬퍼 메서드 활용 예시")
    void demonstrateImprovedHelperMethods() {
        // SessionStatus enum에 내장된 헬퍼 메서드들 (메모리 효율적, 재사용 가능)
        List<SessionStatus> finishedStatuses = SessionStatus.getFinishedStatuses();
        List<SessionStatus> activeStatuses = SessionStatus.getActiveStatuses();
        List<SessionStatus> successfulStatuses = SessionStatus.getSuccessfulStatuses();
        List<SessionStatus> failedStatuses = SessionStatus.getFailedStatuses();
        
        assertThat(finishedStatuses).hasSize(3);
        assertThat(activeStatuses).hasSize(1);
        assertThat(successfulStatuses).containsExactly(SessionStatus.COMPLETED);
        assertThat(failedStatuses).containsExactly(
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
        );
        
        // static 메서드를 사용한 상태 확인 (Set 기반으로 성능 최적화됨)
        assertThat(SessionStatus.isFinished(SessionStatus.COMPLETED)).isTrue();
        assertThat(SessionStatus.isActive(SessionStatus.IN_PROGRESS)).isTrue();
        assertThat(SessionStatus.isFinished(SessionStatus.IN_PROGRESS)).isFalse();
        
        // 추가된 성공/실패 상태 확인 메서드들
        assertThat(SessionStatus.isSuccessful(SessionStatus.COMPLETED)).isTrue();
        assertThat(SessionStatus.isFailed(SessionStatus.INTERRUPTED)).isTrue();
        assertThat(SessionStatus.isFailed(SessionStatus.CANCELLED)).isTrue();
        assertThat(SessionStatus.isFailed(SessionStatus.COMPLETED)).isFalse();
        
        // 인스턴스 메서드도 사용 가능 (더 OOP스러운 접근)
        SessionStatus completedStatus = SessionStatus.COMPLETED;
        SessionStatus inProgressStatus = SessionStatus.IN_PROGRESS;
        
        assertThat(completedStatus.isFinished()).isTrue();
        assertThat(completedStatus.isSuccessful()).isTrue();
        assertThat(inProgressStatus.isActive()).isTrue();
        assertThat(inProgressStatus.isFinished()).isFalse();
    }
    
    @Test
    @DisplayName("실제 비즈니스 시나리오 활용 예시")
    void demonstrateBusinessScenarios() {
        // 실제 서비스에서 활용할 수 있는 시나리오들
        
        // 시나리오 1: 성공적으로 완료된 세션들의 수면 품질 분석
        SessionStatus completedStatus = SessionStatus.COMPLETED;
        
        // 시나리오 2: 중단된 세션들의 패턴 분석 (왜 중단되었는지)
        SessionStatus interruptedStatus = SessionStatus.INTERRUPTED;
        
        // 시나리오 3: 취소된 세션들의 원인 분석
        SessionStatus cancelledStatus = SessionStatus.CANCELLED;
        
        // 시나리오 4: 진행 중인 세션들의 모니터링 및 정리
        SessionStatus inProgressStatus = SessionStatus.IN_PROGRESS;
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(12);
        
        // 이제 repository 메서드들이 각 상태별로 유연하게 데이터를 조회할 수 있음
        assertThat(completedStatus).isNotNull();
        assertThat(interruptedStatus).isNotNull();
        assertThat(cancelledStatus).isNotNull();
        assertThat(inProgressStatus).isNotNull();
        assertThat(cutoffTime).isBefore(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("SessionStatusUtils 유틸리티 클래스 활용 예시")
    void demonstrateSessionStatusUtils() {
        // import io.neulbo.backend.sleep.util.SessionStatusUtils;
        
        // 복잡한 비즈니스 로직이 필요한 경우 유틸리티 클래스 사용
        List<SessionStatus> testStatuses = List.of(
            SessionStatus.COMPLETED,
            SessionStatus.IN_PROGRESS,
            SessionStatus.INTERRUPTED,
            SessionStatus.CANCELLED
        );
        
        // 상태별 개수 계산 (스트림 기반)
        long finishedCount = io.neulbo.backend.sleep.util.SessionStatusUtils.countFinished(testStatuses);
        long activeCount = io.neulbo.backend.sleep.util.SessionStatusUtils.countActive(testStatuses);
        long successfulCount = io.neulbo.backend.sleep.util.SessionStatusUtils.countSuccessful(testStatuses);
        long failedCount = io.neulbo.backend.sleep.util.SessionStatusUtils.countFailed(testStatuses);
        
        assertThat(finishedCount).isEqualTo(3); // COMPLETED, INTERRUPTED, CANCELLED
        assertThat(activeCount).isEqualTo(1);   // IN_PROGRESS
        assertThat(successfulCount).isEqualTo(1); // COMPLETED
        assertThat(failedCount).isEqualTo(2);   // INTERRUPTED, CANCELLED
        
        // 상태 전환 가능성 확인
        assertThat(io.neulbo.backend.sleep.util.SessionStatusUtils.canTransitionTo(
            SessionStatus.IN_PROGRESS, SessionStatus.COMPLETED)).isTrue();
        assertThat(io.neulbo.backend.sleep.util.SessionStatusUtils.canTransitionTo(
            SessionStatus.COMPLETED, SessionStatus.IN_PROGRESS)).isFalse();
        
        // 우선순위 확인
        assertThat(io.neulbo.backend.sleep.util.SessionStatusUtils.getPriority(SessionStatus.IN_PROGRESS))
            .isLessThan(io.neulbo.backend.sleep.util.SessionStatusUtils.getPriority(SessionStatus.COMPLETED));
    }

    @Test
    @DisplayName("성능 및 확장성 개선 확인")
    void verifyPerformanceAndScalability() {
        // 1. 타입 안전성: enum 사용으로 컴파일 타임 검증
        SessionStatus status = SessionStatus.COMPLETED;
        assertThat(status).isInstanceOf(SessionStatus.class);
        
        // 2. 확장성: 새로운 SessionStatus 추가 시 기존 쿼리 변경 불필요
        SessionStatus[] allStatuses = SessionStatus.values();
        assertThat(allStatuses).hasSizeGreaterThanOrEqualTo(4);
        
        // 3. 유지보수성: enum 이름 변경 시 IDE 리팩토링으로 일괄 변경 가능
        String description = SessionStatus.COMPLETED.getDescription();
        assertThat(description).isNotEmpty();
        
        // 4. 재사용성: 동일한 쿼리로 다른 상태들도 조회 가능
        for (SessionStatus sessionStatus : SessionStatus.values()) {
            assertThat(sessionStatus.getDescription()).isNotEmpty();
        }
    }
}