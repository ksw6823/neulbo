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
     * 카테고리별 음악 개수 조회
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.musicList m WHERE c.isActive = true AND m.isActive = true")
    List<Category> findActiveCategoriesWithActiveMusic();

    /**
     * 특정 카테고리의 활성화된 음악 개수
     */
    @Query("SELECT COUNT(m) FROM Music m WHERE m.category.id = :categoryId AND m.isActive = true")
    Long countActiveMusicByCategoryId(UUID categoryId);
} 