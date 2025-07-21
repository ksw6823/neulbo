package io.neulbo.backend.challenge.service;

import io.neulbo.backend.challenge.domain.Challenge;
import io.neulbo.backend.challenge.domain.UserChallenge;
import io.neulbo.backend.challenge.dto.ChallengeResponse;
import io.neulbo.backend.challenge.dto.UserChallengeResponse;
import io.neulbo.backend.challenge.repository.ChallengeRepository;
import io.neulbo.backend.challenge.repository.UserChallengeRepository;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserService userService;

    /**
     * 모든 활성 챌린지 조회
     */
    public List<ChallengeResponse> getAllActiveChallenges() {
        List<Challenge> challenges = challengeRepository.findByIsActiveTrue();
        return challenges.stream()
                .map(ChallengeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 챌린지 참여
     */
    @Transactional
    public UserChallengeResponse joinChallenge(UUID userId, Long challengeId) {
        // 사용자 존재 확인
        User user = userService.findUserById(userId);
        
        // 챌린지 존재 확인
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("챌린지를 찾을 수 없습니다: " + challengeId));

        // 이미 참여 중인지 확인
        if (userChallengeRepository.existsByUserIdAndChallengeIdAndStatus(
                userId, challengeId, UserChallenge.ChallengeStatus.ACTIVE)) {
            throw new IllegalStateException("이미 참여 중인 챌린지입니다");
        }

        // 챌린지 참여 생성
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(challenge.getDurationDays());

        UserChallenge userChallenge = UserChallenge.builder()
                .userId(userId)
                .challenge(challenge)
                .status(UserChallenge.ChallengeStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .completedDays(0)
                .build();

        UserChallenge savedUserChallenge = userChallengeRepository.save(userChallenge);
        log.info("챌린지 참여 성공 - User ID: {}, Challenge ID: {}", userId, challengeId);

        return UserChallengeResponse.from(savedUserChallenge);
    }

    /**
     * 사용자의 활성 챌린지 목록 조회
     */
    public List<UserChallengeResponse> getMyActiveChallenges(UUID userId) {
        List<UserChallenge> userChallenges = userChallengeRepository
                .findByUserIdAndStatus(userId, UserChallenge.ChallengeStatus.ACTIVE);

        return userChallenges.stream()
                .map(UserChallengeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 챌린지의 진행 상황 조회
     */
    public UserChallengeResponse getChallengeProgress(UUID userId, Long challengeId) {
        UserChallenge userChallenge = userChallengeRepository
                .findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new IllegalArgumentException("참여 중인 챌린지가 없습니다"));

        return UserChallengeResponse.from(userChallenge);
    }

    /**
     * 챌린지 완료 처리 (수면 데이터 기반)
     */
    @Transactional
    public void processChallengeCompletion(UUID userId, LocalDate date) {
        List<UserChallenge> activeChallenges = userChallengeRepository
                .findByUserIdAndStatus(userId, UserChallenge.ChallengeStatus.ACTIVE);

        for (UserChallenge userChallenge : activeChallenges) {
            // 오늘 날짜가 챌린지 기간 내인지 확인
            if (!date.isBefore(userChallenge.getStartDate()) && 
                !date.isAfter(userChallenge.getEndDate())) {
                
                // 챌린지 달성 로직 (예시: 간단한 버전)
                boolean isAchievedToday = checkChallengeAchievement(userChallenge, date);
                
                if (isAchievedToday) {
                    userChallenge.addCompletedDay();
                    
                    // 챌린지 완료 시 포인트 지급
                    if (userChallenge.isCompleted()) {
                        giveRewardPoints(userId, userChallenge.getChallenge().getRewardPoints());
                        log.info("챌린지 완료! User ID: {}, Challenge ID: {}, Points: {}", 
                                userId, userChallenge.getChallenge().getId(), 
                                userChallenge.getChallenge().getRewardPoints());
                    }
                }
            }
        }
    }

    /**
     * 챌린지 달성 여부 확인 (임시 구현)
     */
    private boolean checkChallengeAchievement(UserChallenge userChallenge, LocalDate date) {
        // TODO: 실제 수면 데이터를 기반으로 달성 여부 판단
        // 지금은 간단히 true 반환 (데모용)
        return true;
    }

    /**
     * 포인트 지급
     */
    private void giveRewardPoints(UUID userId, Integer points) {
        User user = userService.findUserById(userId);
        user.addPoints(points);
        log.info("포인트 지급 - User ID: {}, Points: {}", userId, points);
    }
} 