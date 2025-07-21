package io.neulbo.backend.music.repository;

import io.neulbo.backend.music.domain.Music;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MusicRepository extends JpaRepository<Music, UUID> {

    /**
     * 활성화된 음악 목록을 페이지네이션으로 조회
     */
    Page<Music> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 카테고리별 활성화된 음악 조회
     */
    Page<Music> findByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(UUID categoryId, Pageable pageable);

    /**
     * 카테고리별 활성화된 음악 조회 (리스트)
     */
    List<Music> findByCategoryIdAndIsActiveTrueOrderByTitleAsc(UUID categoryId);

    /**
     * 음악 제목으로 검색
     */
    Page<Music> findByTitleContainingIgnoreCaseAndIsActiveTrueOrderByTitleAsc(String title, Pageable pageable);

    /**
     * 아티스트명으로 검색
     */
    Page<Music> findByArtistContainingIgnoreCaseAndIsActiveTrueOrderByTitleAsc(String artist, Pageable pageable);

    /**
     * 음악 제목 또는 아티스트명으로 검색
     */
    @Query("SELECT m FROM Music m WHERE m.isActive = true AND " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.title ASC")
    Page<Music> searchByTitleOrArtist(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 인기 음악 조회 (재생수 기준)
     */
    @Query("SELECT m FROM Music m WHERE m.isActive = true ORDER BY m.playCount DESC")
    Page<Music> findPopularMusic(Pageable pageable);

    /**
     * 최신 음악 조회 (별칭)
     */
    default Page<Music> findLatestMusic(Pageable pageable) {
        return findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    /**
     * 무료 음악만 조회
     */
    Page<Music> findByIsPremiumFalseAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 프리미엄 음악만 조회
     */
    Page<Music> findByIsPremiumTrueAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 카테고리의 인기 음악
     */
    @Query("SELECT m FROM Music m WHERE m.category.id = :categoryId AND m.isActive = true ORDER BY m.playCount DESC")
    List<Music> findPopularMusicByCategory(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * 재생시간 범위로 음악 조회
     */
    @Query("SELECT m FROM Music m WHERE m.isActive = true AND " +
           "m.durationSeconds BETWEEN :minDuration AND :maxDuration " +
           "ORDER BY m.createdAt DESC")
    Page<Music> findByDurationRange(@Param("minDuration") Integer minDuration, 
                                    @Param("maxDuration") Integer maxDuration, 
                                    Pageable pageable);
} 