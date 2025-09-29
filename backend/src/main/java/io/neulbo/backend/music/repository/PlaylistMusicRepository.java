package io.neulbo.backend.music.repository;

import io.neulbo.backend.music.domain.PlaylistMusic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistMusicRepository extends JpaRepository<PlaylistMusic, UUID> {

    /**
     * 플레이리스트의 음악 목록 조회 (정렬 순서대로)
     */
    List<PlaylistMusic> findByPlaylistIdOrderBySortOrderAsc(UUID playlistId);

    /**
     * 플레이리스트의 음악 목록 조회 (추가 시간순)
     */
    List<PlaylistMusic> findByPlaylistIdOrderByAddedAtAsc(UUID playlistId);

    /**
     * 특정 플레이리스트에 특정 음악이 있는지 확인
     */
    boolean existsByPlaylistIdAndMusicId(UUID playlistId, UUID musicId);

    /**
     * 특정 플레이리스트의 특정 음악 조회
     */
    Optional<PlaylistMusic> findByPlaylistIdAndMusicId(UUID playlistId, UUID musicId);

    /**
     * 플레이리스트의 음악 개수
     */
    Long countByPlaylistId(UUID playlistId);

    /**
     * 특정 음악이 포함된 플레이리스트 목록
     */
    List<PlaylistMusic> findByMusicIdOrderByAddedAtDesc(UUID musicId);

    /**
     * 플레이리스트의 마지막 정렬 순서 조회
     */
    @Query("SELECT MAX(pm.sortOrder) FROM PlaylistMusic pm WHERE pm.playlist.id = :playlistId")
    Optional<Integer> findMaxSortOrderByPlaylistId(@Param("playlistId") UUID playlistId);

    /**
     * 특정 플레이리스트의 음악들을 정렬 순서대로 조회 (음악 정보 포함)
     */
    @Query("SELECT pm FROM PlaylistMusic pm " +
           "JOIN FETCH pm.music m " +
           "JOIN FETCH m.category c " +
           "WHERE pm.playlist.id = :playlistId " +
           "ORDER BY pm.sortOrder ASC")
    List<PlaylistMusic> findByPlaylistIdWithMusicAndCategory(@Param("playlistId") UUID playlistId);

    /**
     * 사용자의 모든 플레이리스트에서 특정 음악 제거
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PlaylistMusic pm WHERE pm.music.id = :musicId AND pm.playlist.user.id = :userId")
    void deleteByMusicIdAndUserId(@Param("musicId") UUID musicId, @Param("userId") UUID userId);

    /**
     * 플레이리스트에서 음악 제거
     */
    void deleteByPlaylistIdAndMusicId(UUID playlistId, UUID musicId);

    /**
     * 플레이리스트의 모든 음악 제거
     */
    void deleteByPlaylistId(UUID playlistId);
} 