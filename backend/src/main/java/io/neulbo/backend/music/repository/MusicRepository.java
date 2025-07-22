package io.neulbo.backend.music.repository;

import io.neulbo.backend.music.domain.Music;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 재생 횟수를 원자적으로 증가시킴 (동시성 안전)
     * 
     * 이 메서드는 데이터베이스 레벨에서 원자적으로 playCount를 증가시켜
     * 동시 접근 시에도 정확한 카운트를 보장합니다.
     * 
     * 사용 예시:
     * int updatedRows = musicRepository.incrementPlayCountAtomically(musicId);
     * if (updatedRows == 0) {
     *     // 음악이 존재하지 않거나 업데이트 실패
     * }
     * 
     * @param musicId 음악 ID
     * @return 업데이트된 행 수 (성공 시 1, 실패 시 0)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Music m SET m.playCount = m.playCount + 1, m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :musicId")
    int incrementPlayCountAtomically(@Param("musicId") UUID musicId);
} 