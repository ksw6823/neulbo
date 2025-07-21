package io.neulbo.backend.music.controller;

import io.neulbo.backend.music.dto.CategoryResponse;
import io.neulbo.backend.music.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 활성화된 카테고리 목록 조회
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        
        log.info("활성화된 카테고리 목록 조회 요청");
        
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        
        return ResponseEntity.ok(categories);
    }

    /**
     * 모든 카테고리 목록 조회 (관리자용)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        
        log.info("모든 카테고리 목록 조회 요청 (관리자)");
        
        List<CategoryResponse> categories = categoryService.getAllCategories();
        
        return ResponseEntity.ok(categories);
    }

    /**
     * 특정 카테고리 조회
     */
    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID categoryId) {
        
        log.info("카테고리 조회 요청, categoryId: {}", categoryId);
        
        CategoryResponse category = categoryService.getCategoryById(categoryId);
        
        return ResponseEntity.ok(category);
    }

    /**
     * 카테고리명으로 조회
     */
    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CategoryResponse> getCategoryByName(@PathVariable String name) {
        
        log.info("카테고리 이름으로 조회 요청, name: {}", name);
        
        CategoryResponse category = categoryService.getCategoryByName(name);
        
        return ResponseEntity.ok(category);
    }
} 