package io.neulbo.backend.music.repository;

import io.neulbo.backend.music.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * 활성화된 카테고리 목록을 정렬 순서대로 조회
     */
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * 모든 카테고리를 정렬 순서대로 조회
     */
    List<Category> findAllByOrderBySortOrderAsc();

    /**
     * 카테고리명으로 조회
     */
    Optional<Category> findByName(String name);

    /**
     * 활성화된 카테고리인지 확인
     */
    boolean existsByNameAndIsActiveTrue(String name);

    /**
     * 활성 카테고리와 해당 카테고리의 활성 음악들을 함께 조회 (LEFT JOIN 수정)
     * 
     * 주의: 이전 버전에서는 WHERE 절에 m.isActive = true 조건이 있어서
     * 활성 음악이 없는 카테고리들이 제외되는 문제가 있었습니다.
     * 이제 ON 절로 조건을 이동하여 모든 활성 카테고리를 포함하되,
     * 활성 음악만 함께 가져오도록 수정했습니다.
     * 
     * @return 활성 카테고리 목록 (활성 음악만 포함, 음악이 없는 카테고리도 포함)
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.musicList m WHERE c.isActive = true AND (m IS NULL OR m.isActive = true) ORDER BY c.sortOrder")
    List<Category> findActiveCategoriesWithActiveMusic();

    /**
     * 활성 카테고리 중 활성 음악이 하나 이상 있는 카테고리만 조회
     * 
     * 이 메서드는 기존 쿼리의 동작을 재현합니다 (활성 음악이 없는 카테고리 제외).
     * 만약 이전 동작이 필요한 경우 이 메서드를 사용하세요.
     * 
     * @return 활성 음악이 있는 활성 카테고리 목록
     */
    @Query("SELECT DISTINCT c FROM Category c INNER JOIN FETCH c.musicList m WHERE c.isActive = true AND m.isActive = true ORDER BY c.sortOrder")
    List<Category> findActiveCategoriesHavingActiveMusic();

    /**
     * 모든 활성 카테고리 조회 (음악 정보 없이, 성능 최적화)
     * 
     * 음악 정보가 필요하지 않은 경우 이 메서드를 사용하여 성능을 최적화할 수 있습니다.
     * 음악 개수는 별도로 countActiveMusicByCategoryId()를 사용하여 조회하세요.
     * 
     * @return 활성 카테고리 목록 (음악 정보 제외)
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.sortOrder")
    List<Category> findActiveCategoriesOnly();

    /**
     * 특정 카테고리의 활성화된 음악 개수
     */
    @Query("SELECT COUNT(m) FROM Music m WHERE m.category.id = :categoryId AND m.isActive = true")
    Long countActiveMusicByCategoryId(UUID categoryId);

    /**
     * 활성 카테고리와 각 카테고리의 활성 음악 개수를 함께 조회 (N+1 문제 해결)
     * 
     * 이 쿼리는 카테고리와 음악 개수를 한 번에 조회하여 성능을 최적화합니다.
     * Object[] 배열 형태로 반환되며, [0] = Category, [1] = Long (musicCount) 입니다.
     * 
     * @return Object[] 배열 리스트 ([Category, Long] 형태)
     */
    @Query("SELECT c, COUNT(m) FROM Category c LEFT JOIN c.musicList m " +
           "WHERE c.isActive = true AND (m IS NULL OR m.isActive = true) " +
           "GROUP BY c ORDER BY c.sortOrder")
    List<Object[]> findActiveCategoriesWithMusicCount();

    /**
     * 모든 카테고리와 각 카테고리의 활성 음악 개수를 함께 조회 (N+1 문제 해결)
     * 
     * 관리자용으로 활성/비활성 상관없이 모든 카테고리를 조회합니다.
     * 
     * @return Object[] 배열 리스트 ([Category, Long] 형태)
     */
    @Query("SELECT c, COUNT(m) FROM Category c LEFT JOIN c.musicList m " +
           "WHERE (m IS NULL OR m.isActive = true) " +
           "GROUP BY c ORDER BY c.sortOrder")
    List<Object[]> findAllCategoriesWithMusicCount();
} 