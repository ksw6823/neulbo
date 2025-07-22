package io.neulbo.backend.music.repository;

import io.neulbo.backend.music.domain.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    /**
     * 사용자별 플레이리스트 조회
     */
    List<Playlist> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 사용자별 플레이리스트 조회 (페이지네이션)
     */
    Page<Playlist> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 사용자의 기본 플레이리스트 조회
     */
    Optional<Playlist> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * 공개 플레이리스트 조회
     */
    Page<Playlist> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 플레이리스트 이름으로 검색 (공개된 것만)
     */
    Page<Playlist> findByNameContainingIgnoreCaseAndIsPublicTrueOrderByNameAsc(String name, Pageable pageable);

    /**
     * 사용자의 플레이리스트 이름으로 검색
     */
    Page<Playlist> findByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(UUID userId, String name, Pageable pageable);

    /**
     * 사용자별 플레이리스트 개수
     */
    Long countByUserId(UUID userId);

    /**
     * 사용자의 기본 플레이리스트 존재 여부
     */
    boolean existsByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * 인기 플레이리스트 (음악 개수 기준)
     */
    @Query("SELECT p FROM Playlist p WHERE p.isPublic = true ORDER BY p.musicCount DESC")
    Page<Playlist> findPopularPublicPlaylists(Pageable pageable);

    /**
     * 특정 사용자의 특정 이름 플레이리스트 존재 여부
     */
    boolean existsByUserIdAndName(UUID userId, String name);

    /**
     * 사용자의 전체 음악 재생시간
     */
    @Query("SELECT SUM(p.totalDurationSeconds) FROM Playlist p WHERE p.user.id = :userId")
    Long getTotalDurationByUserId(@Param("userId") UUID userId);
} 