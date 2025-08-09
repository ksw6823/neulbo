package io.neulbo.backend.sleep.util;

import io.neulbo.backend.sleep.domain.SessionStatus;

import java.util.List;
import java.util.Set;

/**
 * SessionStatus 관련 유틸리티 클래스
 * 대안적 접근 방식으로, SessionStatus enum에 직접 메서드를 추가하는 것을 선호하지만
 * 복잡한 비즈니스 로직이나 확장성이 필요한 경우 이런 유틸리티 클래스도 유용할 수 있습니다.
 */
public final class SessionStatusUtils {

    // private constructor to prevent instantiation
    private SessionStatusUtils() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다");
    }

    // 메모리 효율성을 위한 static final 상수들
    private static final List<SessionStatus> FINISHED_STATUSES = List.of(
            SessionStatus.COMPLETED, 
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
    );
    
    private static final List<SessionStatus> ACTIVE_STATUSES = List.of(SessionStatus.IN_PROGRESS);
    
    private static final List<SessionStatus> SUCCESSFUL_STATUSES = List.of(SessionStatus.COMPLETED);
    
    private static final List<SessionStatus> FAILED_STATUSES = List.of(
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
    );

    // Set으로도 제공하여 contains 연산의 성능 향상
    private static final Set<SessionStatus> FINISHED_STATUSES_SET = Set.of(
            SessionStatus.COMPLETED, 
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
    );
    
    private static final Set<SessionStatus> ACTIVE_STATUSES_SET = Set.of(SessionStatus.IN_PROGRESS);
    
    private static final Set<SessionStatus> SUCCESSFUL_STATUSES_SET = Set.of(SessionStatus.COMPLETED);
    
    private static final Set<SessionStatus> FAILED_STATUSES_SET = Set.of(
            SessionStatus.INTERRUPTED, 
            SessionStatus.CANCELLED
    );

    /**
     * 완료된 상태들을 반환 (성공/실패 포함)
     * @return 완료된 상태 리스트 (COMPLETED, INTERRUPTED, CANCELLED)
     */
    public static List<SessionStatus> getFinishedStatuses() {
        return FINISHED_STATUSES;
    }

    /**
     * 활성 상태들을 반환
     * @return 활성 상태 리스트 (IN_PROGRESS)
     */
    public static List<SessionStatus> getActiveStatuses() {
        return ACTIVE_STATUSES;
    }

    /**
     * 성공적으로 완료된 상태들을 반환
     * @return 성공 상태 리스트 (COMPLETED)
     */
    public static List<SessionStatus> getSuccessfulStatuses() {
        return SUCCESSFUL_STATUSES;
    }

    /**
     * 실패로 종료된 상태들을 반환
     * @return 실패 상태 리스트 (INTERRUPTED, CANCELLED)
     */
    public static List<SessionStatus> getFailedStatuses() {
        return FAILED_STATUSES;
    }

    /**
     * 주어진 상태가 완료된 상태인지 확인
     * @param status 확인할 상태
     * @return 완료된 상태이면 true, 그렇지 않으면 false
     */
    public static boolean isFinished(SessionStatus status) {
        return FINISHED_STATUSES_SET.contains(status);
    }

    /**
     * 주어진 상태가 활성 상태인지 확인
     * @param status 확인할 상태
     * @return 활성 상태이면 true, 그렇지 않으면 false
     */
    public static boolean isActive(SessionStatus status) {
        return ACTIVE_STATUSES_SET.contains(status);
    }

    /**
     * 주어진 상태가 성공적으로 완료된 상태인지 확인
     * @param status 확인할 상태
     * @return 성공 상태이면 true, 그렇지 않으면 false
     */
    public static boolean isSuccessful(SessionStatus status) {
        return SUCCESSFUL_STATUSES_SET.contains(status);
    }

    /**
     * 주어진 상태가 실패로 종료된 상태인지 확인
     * @param status 확인할 상태
     * @return 실패 상태이면 true, 그렇지 않으면 false
     */
    public static boolean isFailed(SessionStatus status) {
        return FAILED_STATUSES_SET.contains(status);
    }

    /**
     * 상태별 개수를 계산하는 헬퍼 메서드들
     */
    public static long countFinished(List<SessionStatus> statuses) {
        return statuses.stream().filter(SessionStatusUtils::isFinished).count();
    }

    public static long countActive(List<SessionStatus> statuses) {
        return statuses.stream().filter(SessionStatusUtils::isActive).count();
    }

    public static long countSuccessful(List<SessionStatus> statuses) {
        return statuses.stream().filter(SessionStatusUtils::isSuccessful).count();
    }

    public static long countFailed(List<SessionStatus> statuses) {
        return statuses.stream().filter(SessionStatusUtils::isFailed).count();
    }

    /**
     * 상태 전환 관련 헬퍼 메서드들
     */
    public static boolean canTransitionTo(SessionStatus from, SessionStatus to) {
        // 비즈니스 규칙에 따른 상태 전환 가능성 확인
        if (from == SessionStatus.IN_PROGRESS) {
            return to == SessionStatus.COMPLETED || to == SessionStatus.INTERRUPTED || to == SessionStatus.CANCELLED;
        }
        return false; // 완료된 상태에서는 다른 상태로 전환 불가
    }

    /**
     * 상태별 우선순위 (정렬이나 필터링에 사용)
     */
    public static int getPriority(SessionStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 1;    // 가장 높은 우선순위
            case COMPLETED -> 2;      // 성공 완료
            case INTERRUPTED -> 3;    // 중단됨
            case CANCELLED -> 4;      // 취소됨 (가장 낮은 우선순위)
        };
    }
}