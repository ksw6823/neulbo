package io.neulbo.backend.music.service;

import io.neulbo.backend.music.domain.Category;
import io.neulbo.backend.music.dto.CategoryResponse;
import io.neulbo.backend.music.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 카테고리 서비스
 * 
 * 성능 최적화:
 * - getActiveCategories(), getAllCategories() 메서드는 N+1 쿼리 문제를 해결하기 위해
 *   JOIN과 GROUP BY를 사용한 단일 쿼리로 카테고리와 음악 개수를 한 번에 조회합니다.
 * - 기존: 1 + N번 쿼리 (카테고리 목록 1번 + 각 카테고리당 음악 개수 N번)
 * - 개선: 1번 쿼리 (카테고리와 음악 개수를 한 번에 조회)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 활성화된 카테고리 목록 조회 (N+1 문제 해결됨)
     * 
     * 성능 개선:
     * - 기존: 카테고리 N개 → 1 + N번 쿼리 실행
     * - 개선: 카테고리 N개 → 1번 쿼리 실행 (JOIN과 GROUP BY 사용)
     * 
     * @return 활성 카테고리 목록 (음악 개수 포함)
     */
    public List<CategoryResponse> getActiveCategories() {
        log.debug("활성화된 카테고리 목록 조회 - 최적화된 쿼리 사용");
        
        return categoryRepository.findActiveCategoriesWithMusicCount()
                .stream()
                .map(result -> {
                    Category category = (Category) result[0];
                    Long musicCount = (Long) result[1];
                    return CategoryResponse.from(category, musicCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 모든 카테고리 목록 조회 (관리자용, N+1 문제 해결됨)
     * 
     * 성능 개선:
     * - 기존: 카테고리 N개 → 1 + N번 쿼리 실행  
     * - 개선: 카테고리 N개 → 1번 쿼리 실행 (JOIN과 GROUP BY 사용)
     * 
     * @return 모든 카테고리 목록 (음악 개수 포함)
     */
    public List<CategoryResponse> getAllCategories() {
        log.debug("모든 카테고리 목록 조회 - 최적화된 쿼리 사용");
        
        return categoryRepository.findAllCategoriesWithMusicCount()
                .stream()
                .map(result -> {
                    Category category = (Category) result[0];
                    Long musicCount = (Long) result[1];
                    return CategoryResponse.from(category, musicCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리 조회
     */
    public CategoryResponse getCategoryById(UUID categoryId) {
        log.debug("카테고리 조회, categoryId: {}", categoryId);
        
        return categoryRepository.findById(categoryId)
                .map(category -> {
                    Long musicCount = categoryRepository.countActiveMusicByCategoryId(category.getId());
                    return CategoryResponse.from(category, musicCount);
                })
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryId));
    }

    /**
     * 카테고리명으로 조회
     */
    public CategoryResponse getCategoryByName(String name) {
        log.debug("카테고리 이름으로 조회, name: {}", name);
        
        return categoryRepository.findByName(name)
                .map(category -> {
                    Long musicCount = categoryRepository.countActiveMusicByCategoryId(category.getId());
                    return CategoryResponse.from(category, musicCount);
                })
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + name));
    }


} 