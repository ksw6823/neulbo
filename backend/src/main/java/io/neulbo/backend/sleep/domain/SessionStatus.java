package io.neulbo.backend.sleep.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    INTERRUPTED("중단됨"),
    CANCELLED("취소됨");

    private final String description;

    // 상태별 그룹핑을 위한 static final 상수들 (메모리 효율성을 위해)
    private static final List<SessionStatus> FINISHED_STATUSES = List.of(COMPLETED, INTERRUPTED, CANCELLED);
    private static final List<SessionStatus> ACTIVE_STATUSES = List.of(IN_PROGRESS);
    private static final List<SessionStatus> SUCCESSFUL_STATUSES = List.of(COMPLETED);
    private static final List<SessionStatus> FAILED_STATUSES = List.of(INTERRUPTED, CANCELLED);
    
    // Set으로도 제공하여 contains 연산의 성능 향상
    private static final Set<SessionStatus> FINISHED_STATUSES_SET = Set.of(COMPLETED, INTERRUPTED, CANCELLED);
    private static final Set<SessionStatus> ACTIVE_STATUSES_SET = Set.of(IN_PROGRESS);
    private static final Set<SessionStatus> SUCCESSFUL_STATUSES_SET = Set.of(COMPLETED);
    private static final Set<SessionStatus> FAILED_STATUSES_SET = Set.of(INTERRUPTED, CANCELLED);

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
     * 인스턴스 메서드: 현재 상태가 완료된 상태인지 확인
     * @return 완료된 상태이면 true, 그렇지 않으면 false
     */
    public boolean isFinished() {
        return isFinished(this);
    }

    /**
     * 인스턴스 메서드: 현재 상태가 활성 상태인지 확인
     * @return 활성 상태이면 true, 그렇지 않으면 false
     */
    public boolean isActive() {
        return isActive(this);
    }

    /**
     * 인스턴스 메서드: 현재 상태가 성공적으로 완료된 상태인지 확인
     * @return 성공 상태이면 true, 그렇지 않으면 false
     */
    public boolean isSuccessful() {
        return isSuccessful(this);
    }

    /**
     * 인스턴스 메서드: 현재 상태가 실패로 종료된 상태인지 확인
     * @return 실패 상태이면 true, 그렇지 않으면 false
     */
    public boolean isFailed() {
        return isFailed(this);
    }
} 