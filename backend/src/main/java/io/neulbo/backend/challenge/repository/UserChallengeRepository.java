package io.neulbo.backend.challenge.repository;

import io.neulbo.backend.challenge.domain.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {
    
    // 사용자의 활성 챌린지 목록 조회
    List<UserChallenge> findByUserIdAndStatus(UUID userId, UserChallenge.ChallengeStatus status);
    
    // 사용자의 모든 챌린지 참여 내역 조회
    List<UserChallenge> findByUserId(UUID userId);
    
    // 특정 챌린지에 대한 사용자의 참여 내역 조회
    Optional<UserChallenge> findByUserIdAndChallengeId(UUID userId, Long challengeId);
    
    // 사용자가 이미 해당 챌린지에 참여 중인지 확인
    boolean existsByUserIdAndChallengeIdAndStatus(UUID userId, Long challengeId, UserChallenge.ChallengeStatus status);
    
    // 특정 챌린지의 진행률 순 리더보드 (상위 10명)
    @Query("SELECT uc FROM UserChallenge uc " +
           "WHERE uc.challenge.id = :challengeId " +
           "ORDER BY uc.completedDays DESC, uc.createdAt ASC")
    List<UserChallenge> findTop10ByChallengeIdOrderByCompletedDaysDesc(@Param("challengeId") Long challengeId);
} 