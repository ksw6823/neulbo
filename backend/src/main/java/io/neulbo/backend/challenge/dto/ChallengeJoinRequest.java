package io.neulbo.backend.challenge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChallengeJoinRequest {
    
    @NotNull(message = "챌린지 ID는 필수입니다")
    private Long challengeId;
    
    public ChallengeJoinRequest(Long challengeId) {
        this.challengeId = challengeId;
    }
} 