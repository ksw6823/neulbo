package io.neulbo.backend.challenge.repository;

import io.neulbo.backend.challenge.domain.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
    List<UserChallenge> findTop10ByChallengeIdOrderByCompletedDaysDescCreatedAtAsc(Long challengeId);
    
    // 배치 업데이트: 여러 챌린지의 완료 일수 증가
    @Modifying
    @Transactional
    @Query("UPDATE UserChallenge uc SET uc.completedDays = uc.completedDays + 1 " +
           "WHERE uc.id IN :userChallengeIds")
    int incrementCompletedDaysForChallenges(@Param("userChallengeIds") List<Long> userChallengeIds);
    
    // 배치 업데이트: 완료된 챌린지들의 상태를 COMPLETED로 변경
    @Modifying
    @Transactional
    @Query("UPDATE UserChallenge uc SET uc.status = :completedStatus " +
           "WHERE uc.completedDays >= uc.challenge.durationDays " +
           "AND uc.status = :activeStatus")
    int updateCompletedChallengeStatus(@Param("completedStatus") UserChallenge.ChallengeStatus completedStatus,
                                       @Param("activeStatus") UserChallenge.ChallengeStatus activeStatus);
} 