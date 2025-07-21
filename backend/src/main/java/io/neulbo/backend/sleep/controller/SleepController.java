package io.neulbo.backend.sleep.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.sleep.dto.request.EndSleepSessionRequest;
import io.neulbo.backend.sleep.dto.request.MovementDataRequest;
import io.neulbo.backend.sleep.dto.request.StartSleepSessionRequest;
import io.neulbo.backend.sleep.dto.response.SleepSessionResponse;
import io.neulbo.backend.sleep.dto.response.SleepStatisticsResponse;
import io.neulbo.backend.sleep.service.MovementDataService;
import io.neulbo.backend.sleep.service.SleepAnalysisService;
import io.neulbo.backend.sleep.service.SleepSessionService;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sleep")
@RequiredArgsConstructor
@Slf4j
public class SleepController {

    private final SleepSessionService sleepSessionService;
    private final MovementDataService movementDataService;
    private final SleepAnalysisService sleepAnalysisService;
    private final UserService userService;

    /**
     * 수면 세션 시작
     */
    @PostMapping("/sessions/start")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepSessionResponse> startSleepSession(@Valid @RequestBody StartSleepSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        log.info("수면 세션 시작 요청: userId={}", userId);
        SleepSessionResponse response = sleepSessionService.startSleepSession(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 수면 세션 종료
     */
    @PostMapping("/sessions/end")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepSessionResponse> endSleepSession(@Valid @RequestBody EndSleepSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        log.info("수면 세션 종료 요청: userId={}", userId);
        SleepSessionResponse response = sleepSessionService.endSleepSession(user, request);
        
        // 세션 종료 후 자동 분석 실행
        try {
            sleepAnalysisService.analyzeSleepSession(response.getId());
        } catch (Exception e) {
            log.error("수면 세션 자동 분석 실패: sessionId={}", response.getId(), e);
            // 분석 실패해도 세션 종료는 성공으로 처리
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 수면 세션 조회
     */
    @GetMapping("/sessions/current")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepSessionResponse> getCurrentSleepSession() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        SleepSessionResponse response = sleepSessionService.getCurrentSleepSession(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 수면 세션 상세 조회
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepSessionResponse> getSleepSession(@PathVariable Long sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        SleepSessionResponse response = sleepSessionService.getSleepSession(user, sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 수면 세션 목록 조회
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<SleepSessionResponse>> getSleepSessions(@PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        Page<SleepSessionResponse> response = sleepSessionService.getSleepSessions(user, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 수면 세션 조회
     */
    @GetMapping("/sessions/range")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SleepSessionResponse>> getSleepSessionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        List<SleepSessionResponse> response = sleepSessionService.getSleepSessionsByDateRange(user, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 주간 수면 세션 조회
     */
    @GetMapping("/sessions/recent/week")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SleepSessionResponse>> getRecentWeekSleepSessions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        List<SleepSessionResponse> response = sleepSessionService.getRecentWeekSleepSessions(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 월간 수면 세션 조회
     */
    @GetMapping("/sessions/recent/month")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SleepSessionResponse>> getRecentMonthSleepSessions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        List<SleepSessionResponse> response = sleepSessionService.getRecentMonthSleepSessions(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 수면 세션 삭제
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteSleepSession(@PathVariable Long sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        sleepSessionService.deleteSleepSession(user, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 움직임 데이터 저장
     */
    @PostMapping("/movement-data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> saveMovementData(@Valid @RequestBody MovementDataRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        log.info("움직임 데이터 저장 요청: userId={}, 데이터 수={}", userId, request.getMovementData().size());
        movementDataService.saveMovementData(user, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 움직임 통계 조회
     */
    @GetMapping("/sessions/{sessionId}/movement-stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovementDataService.MovementStatistics> getMovementStatistics(@PathVariable Long sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        MovementDataService.MovementStatistics stats = movementDataService.calculateMovementStatistics(user, sessionId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 시간대별 움직임 패턴 조회
     */
    @GetMapping("/sessions/{sessionId}/movement-pattern")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MovementDataService.HourlyMovementPattern>> getHourlyMovementPattern(@PathVariable Long sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        List<MovementDataService.HourlyMovementPattern> pattern = movementDataService.analyzeHourlyMovementPattern(user, sessionId);
        return ResponseEntity.ok(pattern);
    }

    /**
     * 수면 세션 수동 분석
     */
    @PostMapping("/sessions/{sessionId}/analyze")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> analyzeSleepSession(@PathVariable Long sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        // 세션 소유권 확인을 위해 먼저 조회
        sleepSessionService.getSleepSession(user, sessionId);
        
        sleepAnalysisService.analyzeSleepSession(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 수면 통계 조회
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepStatisticsResponse> getSleepStatistics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 수면 통계 조회
     */
    @GetMapping("/statistics/period")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SleepStatisticsResponse> getSleepStatisticsForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findUserById(userId);
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatisticsForPeriod(user, startDate, endDate);
        return ResponseEntity.ok(response);
    }
} 