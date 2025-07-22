package io.neulbo.backend.challenge.repository;

import io.neulbo.backend.challenge.domain.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    
    // 활성화된 챌린지 목록 조회
    List<Challenge> findByIsActiveTrue();
    
    // 챌린지 타입별 조회
    List<Challenge> findByTypeAndIsActiveTrue(Challenge.ChallengeType type);
} 