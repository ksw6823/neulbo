package io.neulbo.backend.sleep.service;

import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.dto.request.EndSleepSessionRequest;
import io.neulbo.backend.sleep.dto.request.StartSleepSessionRequest;
import io.neulbo.backend.sleep.dto.response.SleepSessionResponse;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SleepSessionService {

    // 중단된 세션 정리를 위한 시간 관련 상수
    private static final int STALE_SESSION_HOURS = 24; // 중단된 것으로 간주할 시간 (시간)
    private static final int ASSUMED_SLEEP_HOURS = 8;  // 중단된 세션의 가정 수면 시간 (시간)

    private final SleepSessionRepository sleepSessionRepository;

    /**
     * 수면 세션 시작
     */
    @Transactional
    public SleepSessionResponse startSleepSession(User user, StartSleepSessionRequest request) {
        log.info("사용자 {}의 수면 세션 시작 요청", user.getId());

        // 진행 중인 세션이 있는지 확인
        sleepSessionRepository.findByUserAndSessionStatus(user, SessionStatus.IN_PROGRESS)
                .ifPresent(session -> {
                    throw new BusinessException(ErrorCode.SLEEP_SESSION_ALREADY_IN_PROGRESS);
                });

        // 새로운 수면 세션 생성
        SleepSession session = SleepSession.createSession(
                user,
                request.getSleepStartTime(),
                request.getIntendedSleepDurationMinutes()
        );

        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            session.addNotes(request.getNotes());
        }

        SleepSession savedSession = sleepSessionRepository.save(session);
        log.info("수면 세션 생성 완료: sessionId={}", savedSession.getId());

        return new SleepSessionResponse(savedSession);
    }

    /**
     * 수면 세션 종료
     */
    @Transactional
    public SleepSessionResponse endSleepSession(User user, EndSleepSessionRequest request) {
        log.info("사용자 {}의 수면 세션 종료 요청", user.getId());

        // 진행 중인 세션 조회
        SleepSession session = sleepSessionRepository.findByUserAndSessionStatus(user, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        // 수면 세션 종료
        session.endSleep(request.getSleepEndTime());

        // 수면 품질 업데이트
        if (request.getSleepQuality() != null) {
            session.updateSleepQuality(request.getSleepQuality());
        }

        // 추가 노트
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            session.addNotes(request.getNotes());
        }

        SleepSession savedSession = sleepSessionRepository.save(session);
        log.info("수면 세션 종료 완료: sessionId={}", savedSession.getId());

        return new SleepSessionResponse(savedSession);
    }

    /**
     * 현재 진행 중인 수면 세션 조회
     */
    public SleepSessionResponse getCurrentSleepSession(User user) {
        SleepSession session = sleepSessionRepository.findByUserAndSessionStatus(user, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        return new SleepSessionResponse(session);
    }

    /**
     * 특정 수면 세션 상세 조회
     */
    public SleepSessionResponse getSleepSession(User user, Long sessionId) {
        SleepSession session = sleepSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        // 본인의 세션인지 확인
        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return new SleepSessionResponse(session);
    }

    /**
     * 사용자의 수면 세션 목록 조회 (페이징)
     */
    public Page<SleepSessionResponse> getSleepSessions(User user, Pageable pageable) {
        Page<SleepSession> sessions = sleepSessionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return sessions.map(SleepSessionResponse::createSimple);
    }

    /**
     * 특정 기간의 수면 세션 목록 조회
     */
    public List<SleepSessionResponse> getSleepSessionsByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<SleepSession> sessions = sleepSessionRepository.findByUserAndDateRange(user, startDate, endDate);
        return sessions.stream()
                .map(SleepSessionResponse::createSimple)
                .collect(Collectors.toList());
    }

    /**
     * 최근 7일간의 수면 세션 조회
     */
    public List<SleepSessionResponse> getRecentWeekSleepSessions(User user) {
        LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1);
        List<SleepSession> sessions = sleepSessionRepository.findByUserAndLastWeek(user, weekStart);
        return sessions.stream()
                .map(SleepSessionResponse::createSimple)
                .collect(Collectors.toList());
    }

    /**
     * 최근 30일간의 수면 세션 조회
     */
    public List<SleepSessionResponse> getRecentMonthSleepSessions(User user) {
        LocalDateTime monthStart = LocalDateTime.now().minusMonths(1);
        List<SleepSession> sessions = sleepSessionRepository.findByUserAndLastMonth(user, monthStart);
        return sessions.stream()
                .map(SleepSessionResponse::createSimple)
                .collect(Collectors.toList());
    }

    /**
     * 수면 세션 삭제 (본인만 가능)
     */
    @Transactional
    public void deleteSleepSession(User user, Long sessionId) {
        SleepSession session = sleepSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SESSION_NOT_FOUND));

        // 본인의 세션인지 확인
        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 진행 중인 세션은 삭제 불가
        if (session.isInProgress()) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_ACTIVE_SLEEP_SESSION);
        }

        sleepSessionRepository.delete(session);
        log.info("수면 세션 삭제 완료: sessionId={}", sessionId);
    }

    /**
     * 중단된 세션들 정리 (스케줄러용)
     */
    @Transactional
    public void cleanupStaleSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(STALE_SESSION_HOURS); // 24시간 이상 된 진행 중 세션들
        List<SleepSession> staleSessions = sleepSessionRepository.findStaleInProgressSessions(cutoffTime);
        
        for (SleepSession session : staleSessions) {
            session.endSleep(session.getCreatedAt().plusHours(ASSUMED_SLEEP_HOURS)); // 8시간으로 가정하고 종료
            log.warn("중단된 수면 세션 정리: sessionId={}", session.getId());
        }
        
        if (!staleSessions.isEmpty()) {
            sleepSessionRepository.saveAll(staleSessions);
            log.info("총 {}개의 중단된 세션 정리 완료", staleSessions.size());
        }
    }
} 