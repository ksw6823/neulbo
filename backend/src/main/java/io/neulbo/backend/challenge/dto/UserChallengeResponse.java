package io.neulbo.backend.challenge.dto;

import io.neulbo.backend.challenge.domain.UserChallenge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserChallengeResponse {
    private Long id;
    private UUID userId;
    private ChallengeResponse challenge;
    private UserChallenge.ChallengeStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer completedDays;
    private Double progressPercentage;
    private LocalDateTime createdAt;

    public static UserChallengeResponse from(UserChallenge userChallenge) {
        return UserChallengeResponse.builder()
                .id(userChallenge.getId())
                .userId(userChallenge.getUserId())
                .challenge(ChallengeResponse.from(userChallenge.getChallenge()))
                .status(userChallenge.getStatus())
                .startDate(userChallenge.getStartDate())
                .endDate(userChallenge.getEndDate())
                .completedDays(userChallenge.getCompletedDays())
                .progressPercentage(userChallenge.getProgressPercentage())
                .createdAt(userChallenge.getCreatedAt())
                .build();
    }
} 