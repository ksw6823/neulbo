package io.neulbo.backend.challenge.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.challenge.dto.ChallengeResponse;
import io.neulbo.backend.challenge.dto.UserChallengeResponse;
import io.neulbo.backend.challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * 챌린지 목록 조회
     * GET /challenges
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChallengeResponse>> getChallenges() {
        try {
            List<ChallengeResponse> challenges = challengeService.getAllActiveChallenges();
            return ResponseEntity.ok(challenges);
        } catch (Exception e) {
            log.error("챌린지 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 챌린지 참여
     * POST /challenges/{challengeId}/join
     */
    @PostMapping("/{challengeId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserChallengeResponse> joinChallenge(@PathVariable Long challengeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            UserChallengeResponse response = challengeService.joinChallenge(userId, challengeId);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.error("챌린지 참여 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.error("챌린지 참여 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(409).build(); // Conflict
        } catch (Exception e) {
            log.error("챌린지 참여 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 내 챌린지 진행율 조회
     * GET /challenges/my-progress
     */
    @GetMapping("/my-progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserChallengeResponse>> getMyProgress() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<UserChallengeResponse> challenges = challengeService.getMyActiveChallenges(userId);
            return ResponseEntity.ok(challenges);
        } catch (Exception e) {
            log.error("내 챌린지 진행율 조회 실패 - User ID: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 챌린지 진행율 조회
     * GET /challenges/{challengeId}/progress
     */
    @GetMapping("/{challengeId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserChallengeResponse> getChallengeProgress(@PathVariable Long challengeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            UserChallengeResponse response = challengeService.getChallengeProgress(userId, challengeId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("챌린지 진행율 조회 실패 - User ID: {}, Challenge ID: {}, Error: {}", 
                    userId, challengeId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("챌린지 진행율 조회 실패 - User ID: {}, Challenge ID: {}", userId, challengeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 