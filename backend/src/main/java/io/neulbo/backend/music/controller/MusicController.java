package io.neulbo.backend.music.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.music.dto.MusicResponse;
import io.neulbo.backend.music.service.MusicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    /**
     * 모든 음악 조회 (페이지네이션)
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getAllMusic(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("모든 음악 조회 요청, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<MusicResponse> musicPage = musicService.getAllMusic(pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 카테고리별 음악 조회
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getMusicByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("카테고리별 음악 조회 요청, categoryId: {}, page: {}", categoryId, pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getMusicByCategory(categoryId, pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 음악 검색
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> searchMusic(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("음악 검색 요청, keyword: {}, page: {}", keyword, pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.searchMusic(keyword, pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 인기 음악 조회
     */
    @GetMapping("/popular")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getPopularMusic(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("인기 음악 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getPopularMusic(pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 최신 음악 조회
     */
    @GetMapping("/latest")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getLatestMusic(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("최신 음악 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getLatestMusic(pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 무료 음악 조회
     */
    @GetMapping("/free")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getFreeMusic(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("무료 음악 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getFreeMusic(pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 프리미엄 음악 조회
     */
    @GetMapping("/premium")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getPremiumMusic(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("프리미엄 음악 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getPremiumMusic(pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 특정 음악 상세 조회
     */
    @GetMapping("/{musicId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MusicResponse> getMusicById(@PathVariable UUID musicId) {
        
        log.info("음악 상세 조회 요청, musicId: {}", musicId);
        
        MusicResponse music = musicService.getMusicById(musicId);
        
        return ResponseEntity.ok(music);
    }

    /**
     * 재생시간 범위로 음악 조회
     */
    @GetMapping("/duration")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MusicResponse>> getMusicByDurationRange(
            @RequestParam Integer minDuration,
            @RequestParam Integer maxDuration,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("재생시간 범위 음악 조회 요청, minDuration: {}, maxDuration: {}, page: {}", 
                minDuration, maxDuration, pageable.getPageNumber());
        
        Page<MusicResponse> musicPage = musicService.getMusicByDurationRange(minDuration, maxDuration, pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 카테고리별 인기 음악 조회
     */
    @GetMapping("/category/{categoryId}/popular")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MusicResponse>> getPopularMusicByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("카테고리별 인기 음악 조회 요청, categoryId: {}, limit: {}", categoryId, limit);
        
        List<MusicResponse> musicList = musicService.getPopularMusicByCategory(categoryId, limit);
        
        return ResponseEntity.ok(musicList);
    }

    /**
     * 음악 재생수 증가
     */
    @PostMapping("/{musicId}/play")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> incrementPlayCount(@PathVariable UUID musicId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("음악 재생수 증가 요청, musicId: {}, userId: {}", musicId, userId);
        
        musicService.incrementPlayCount(musicId);
        
        return ResponseEntity.ok("재생수가 증가되었습니다");
    }
} 