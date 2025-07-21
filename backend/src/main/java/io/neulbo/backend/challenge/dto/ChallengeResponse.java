package io.neulbo.backend.challenge.dto;

import io.neulbo.backend.challenge.domain.Challenge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChallengeResponse {
    private Long id;
    private String title;
    private String description;
    private Challenge.ChallengeType type;
    private Integer durationDays;
    private Integer targetValue;
    private Integer rewardPoints;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static ChallengeResponse from(Challenge challenge) {
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .type(challenge.getType())
                .durationDays(challenge.getDurationDays())
                .targetValue(challenge.getTargetValue())
                .rewardPoints(challenge.getRewardPoints())
                .isActive(challenge.getIsActive())
                .createdAt(challenge.getCreatedAt())
                .build();
    }
} 