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
import java.util.ArrayList;
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
     * 특정 챌린지의 리더보드 조회 (상위 10명)
     */
    public List<UserChallengeResponse> getChallengeLeaderboard(Long challengeId) {
        // 챌린지 존재 확인
        challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("챌린지를 찾을 수 없습니다: " + challengeId));

        List<UserChallenge> topChallenges = userChallengeRepository
                .findTop10ByChallengeIdOrderByCompletedDaysDescCreatedAtAsc(challengeId);

        return topChallenges.stream()
                .map(UserChallengeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 챌린지 완료 처리 (수면 데이터 기반) - 개선된 버전
     */
    @Transactional
    public void processChallengeCompletion(UUID userId, LocalDate date) {
        List<UserChallenge> activeChallenges = userChallengeRepository
                .findByUserIdAndStatus(userId, UserChallenge.ChallengeStatus.ACTIVE);

        List<UserChallenge> challengesToUpdate = new ArrayList<>();
        List<UserChallenge> newlyCompletedChallenges = new ArrayList<>();

        for (UserChallenge userChallenge : activeChallenges) {
            // 향상된 가독성: 날짜 범위 및 중복 체크
            if (!userChallenge.isDateWithinChallengePeriod(date)) {
                log.debug("챌린지 기간 외: Challenge ID {}, Date {}", 
                        userChallenge.getChallenge().getId(), date);
                continue;
            }
            
            if (userChallenge.isAlreadyCompletedOnDate(date)) {
                log.debug("이미 완료 처리된 날짜: Challenge ID {}, Date {}", 
                        userChallenge.getChallenge().getId(), date);
                continue;
            }
            
            // 챌린지 달성 여부 확인
            boolean isAchievedToday = checkChallengeAchievement(userChallenge, date);
            
            if (isAchievedToday) {
                // 메모리에서 상태 업데이트 (배치 업데이트 전 준비)
                userChallenge.addCompletedDay(date);
                challengesToUpdate.add(userChallenge);
                
                // 새로 완료된 챌린지 추적
                if (userChallenge.isCompleted()) {
                    newlyCompletedChallenges.add(userChallenge);
                }
            }
        }

        // 배치 업데이트 실행 (성능 최적화)
        if (!challengesToUpdate.isEmpty()) {
            performBatchUpdatesAndRewards(challengesToUpdate, newlyCompletedChallenges, userId);
            log.info("챌린지 완료 처리 완료: User ID {}, Date {}, 처리된 챌린지 수: {}", 
                    userId, date, challengesToUpdate.size());
        }
    }

    /**
     * 배치 업데이트 및 보상 처리
     */
    private void performBatchUpdatesAndRewards(List<UserChallenge> challengesToUpdate, 
                                               List<UserChallenge> newlyCompletedChallenges, 
                                               UUID userId) {
        // 1. 변경된 챌린지들을 일괄 저장
        userChallengeRepository.saveAll(challengesToUpdate);
        
        // 2. 새로 완료된 챌린지들에 대한 보상 처리
        for (UserChallenge completedChallenge : newlyCompletedChallenges) {
            giveRewardPoints(userId, completedChallenge.getChallenge().getRewardPoints());
            log.info("챌린지 완료! User ID: {}, Challenge ID: {}, Points: {}", 
                    userId, completedChallenge.getChallenge().getId(), 
                    completedChallenge.getChallenge().getRewardPoints());
        }
        
        // 3. 완료된 챌린지 상태 배치 업데이트 (추가 안전장치)
        userChallengeRepository.updateCompletedChallengeStatus(
                UserChallenge.ChallengeStatus.COMPLETED, 
                UserChallenge.ChallengeStatus.ACTIVE);
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
     * 포인트 지급 (에러 핸들링 포함)
     */
    private void giveRewardPoints(UUID userId, Integer points) {
        try {
            // 입력값 검증
            if (userId == null) {
                log.error("포인트 지급 실패: 사용자 ID가 null입니다");
                return;
            }
            
            if (points == null || points <= 0) {
                log.error("포인트 지급 실패: 잘못된 포인트 값 - User ID: {}, Points: {}", userId, points);
                return;
            }
            
            // 사용자 조회 및 포인트 지급
            User user = userService.findUserById(userId);
            
            // TODO: User 엔티티에 포인트 시스템이 구현되면 주석 해제
            // user.addPoints(points);
            
            log.info("포인트 지급 성공 - User ID: {}, Points: {}", userId, points);
            
        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없는 경우
            log.error("포인트 지급 실패: 사용자를 찾을 수 없음 - User ID: {}, Points: {}, Error: {}", 
                    userId, points, e.getMessage());
        } catch (RuntimeException e) {
            // 포인트 추가 실패 (예: 최대 한도 초과, 유효성 검사 실패 등)
            log.error("포인트 지급 실패: 포인트 추가 중 오류 - User ID: {}, Points: {}, Error: {}", 
                    userId, points, e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 오류
            log.error("포인트 지급 실패: 예상치 못한 오류 - User ID: {}, Points: {}, Error: {}", 
                    userId, points, e.getMessage(), e);
        }
    }
} 