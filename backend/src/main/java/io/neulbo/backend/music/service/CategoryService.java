package io.neulbo.backend.music.service;

import io.neulbo.backend.music.dto.CategoryResponse;
import io.neulbo.backend.music.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 활성화된 카테고리 목록 조회
     */
    public List<CategoryResponse> getActiveCategories() {
        log.debug("활성화된 카테고리 목록 조회");
        
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(category -> {
                    Long musicCount = categoryRepository.countActiveMusicByCategoryId(category.getId());
                    return CategoryResponse.from(category, musicCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 모든 카테고리 목록 조회 (관리자용)
     */
    public List<CategoryResponse> getAllCategories() {
        log.debug("모든 카테고리 목록 조회");
        
        return categoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(category -> {
                    Long musicCount = categoryRepository.countActiveMusicByCategoryId(category.getId());
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