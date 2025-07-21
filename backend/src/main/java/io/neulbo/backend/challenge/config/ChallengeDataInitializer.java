package io.neulbo.backend.challenge.config;

import io.neulbo.backend.challenge.domain.Challenge;
import io.neulbo.backend.challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChallengeDataInitializer {

    private final ChallengeRepository challengeRepository;

    @Bean
    public ApplicationRunner initializeChallenges() {
        return args -> {
            if (challengeRepository.count() == 0) {
                createDefaultChallenges();
                log.info("기본 챌린지 데이터 생성 완료");
            } else {
                log.info("챌린지 데이터가 이미 존재합니다");
            }
        };
    }

    private void createDefaultChallenges() {
        // 수면 시간 챌린지
        Challenge sleepTimeChallenge = Challenge.builder()
                .title("황금 수면 8시간")
                .description("7일 연속 8시간 이상 수면하기")
                .type(Challenge.ChallengeType.SLEEP_TIME)
                .durationDays(7)
                .targetValue(480) // 8시간 = 480분
                .rewardPoints(100)
                .isActive(true)
                .build();

        // 수면 일관성 챌린지
        Challenge consistencyChallenge = Challenge.builder()
                .title("수면 루틴 마스터")
                .description("14일 동안 일정한 시간에 잠자리 들기")
                .type(Challenge.ChallengeType.CONSISTENCY)
                .durationDays(14)
                .targetValue(30) // 30분 이내 오차
                .rewardPoints(150)
                .isActive(true)
                .build();

        // 기상 시간 챌린지
        Challenge wakeTimeChallenge = Challenge.builder()
                .title("얼리버드 챌린지")
                .description("7일 연속 오전 7시 전에 일어나기")
                .type(Challenge.ChallengeType.WAKE_TIME)
                .durationDays(7)
                .targetValue(420) // 7시 = 420분 (00:00부터 계산)
                .rewardPoints(80)
                .isActive(true)
                .build();

        // 장기 수면 시간 챌린지
        Challenge longSleepChallenge = Challenge.builder()
                .title("수면 마라톤")
                .description("30일 연속 7시간 이상 수면하기")
                .type(Challenge.ChallengeType.SLEEP_TIME)
                .durationDays(30)
                .targetValue(420) // 7시간 = 420분
                .rewardPoints(300)
                .isActive(true)
                .build();

        challengeRepository.save(sleepTimeChallenge);
        challengeRepository.save(consistencyChallenge);
        challengeRepository.save(wakeTimeChallenge);
        challengeRepository.save(longSleepChallenge);

        log.info("4개의 기본 챌린지가 생성되었습니다");
    }
} 