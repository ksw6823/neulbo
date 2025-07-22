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
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/music")
@RequiredArgsConstructor
public class MusicController {

    // 상수 정의
    private static final int DEFAULT_POPULAR_MUSIC_LIMIT = 10;
    private static final int MAX_POPULAR_MUSIC_LIMIT = 100;
    private static final int MIN_POPULAR_MUSIC_LIMIT = 1;
    
    // 검색 키워드 관련 상수
    private static final int MIN_KEYWORD_LENGTH = 1;
    private static final int MAX_KEYWORD_LENGTH = 100;

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
     * 
     * @param keyword 검색 키워드 (1-100자, 공백만으로는 불가)
     * @param pageable 페이징 정보
     * @return 검색 결과 또는 에러 응답
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> searchMusic(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("음악 검색 요청, keyword: '{}', page: {}", keyword, pageable.getPageNumber());
        
        // keyword 파라미터 유효성 검사
        if (keyword == null) {
            log.warn("검색 키워드가 null입니다");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "검색 키워드는 필수입니다",
                               "minLength", MIN_KEYWORD_LENGTH,
                               "maxLength", MAX_KEYWORD_LENGTH));
        }
        
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            log.warn("검색 키워드가 빈 문자열 또는 공백만 포함합니다: '{}'", keyword);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "검색 키워드는 공백만으로 구성될 수 없습니다",
                               "providedKeyword", keyword,
                               "minLength", MIN_KEYWORD_LENGTH,
                               "maxLength", MAX_KEYWORD_LENGTH));
        }
        
        if (trimmedKeyword.length() < MIN_KEYWORD_LENGTH) {
            log.warn("검색 키워드가 너무 짧습니다: '{}', 길이: {}", trimmedKeyword, trimmedKeyword.length());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "검색 키워드는 " + MIN_KEYWORD_LENGTH + "자 이상이어야 합니다",
                               "providedKeyword", trimmedKeyword,
                               "keywordLength", trimmedKeyword.length(),
                               "minLength", MIN_KEYWORD_LENGTH,
                               "maxLength", MAX_KEYWORD_LENGTH));
        }
        
        if (trimmedKeyword.length() > MAX_KEYWORD_LENGTH) {
            log.warn("검색 키워드가 너무 깁니다: '{}', 길이: {}", trimmedKeyword, trimmedKeyword.length());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "검색 키워드는 " + MAX_KEYWORD_LENGTH + "자 이하여야 합니다",
                               "providedKeyword", trimmedKeyword,
                               "keywordLength", trimmedKeyword.length(),
                               "minLength", MIN_KEYWORD_LENGTH,
                               "maxLength", MAX_KEYWORD_LENGTH));
        }
        
        Page<MusicResponse> musicPage = musicService.searchMusic(trimmedKeyword, pageable);
        
        log.info("음악 검색 완료, keyword: '{}', page: {}, 결과 개수: {}", 
                trimmedKeyword, pageable.getPageNumber(), musicPage.getContent().size());
        
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
    public ResponseEntity<?> getMusicByDurationRange(
            @RequestParam Integer minDuration,
            @RequestParam Integer maxDuration,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("재생시간 범위 음악 조회 요청, minDuration: {}, maxDuration: {}, page: {}", 
                minDuration, maxDuration, pageable.getPageNumber());
        
        // 입력값 유효성 검사
        if (minDuration == null) {
            log.warn("최소 재생시간이 null입니다");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "최소 재생시간(minDuration)은 필수 입력값입니다"));
        }
        
        if (maxDuration == null) {
            log.warn("최대 재생시간이 null입니다");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "최대 재생시간(maxDuration)은 필수 입력값입니다"));
        }
        
        if (minDuration < 0) {
            log.warn("최소 재생시간이 음수입니다: {}", minDuration);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "최소 재생시간은 0 이상이어야 합니다"));
        }
        
        if (maxDuration < 0) {
            log.warn("최대 재생시간이 음수입니다: {}", maxDuration);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "최대 재생시간은 0 이상이어야 합니다"));
        }
        
        if (minDuration > maxDuration) {
            log.warn("최소 재생시간이 최대 재생시간보다 큽니다. min: {}, max: {}", minDuration, maxDuration);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "최소 재생시간은 최대 재생시간보다 클 수 없습니다", 
                               "minDuration", minDuration, 
                               "maxDuration", maxDuration));
        }
        
        Page<MusicResponse> musicPage = musicService.getMusicByDurationRange(minDuration, maxDuration, pageable);
        
        return ResponseEntity.ok(musicPage);
    }

    /**
     * 카테고리별 인기 음악 조회
     * 
     * @param categoryId 카테고리 ID
     * @param limit 조회할 음악 개수 (1-100 범위, 기본값: 10)
     * @return 인기 음악 목록 또는 에러 응답
     */
    @GetMapping("/category/{categoryId}/popular")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPopularMusicByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("카테고리별 인기 음악 조회 요청, categoryId: {}, limit: {}", categoryId, limit);
        
        // limit 파라미터 유효성 검사
        if (limit < MIN_POPULAR_MUSIC_LIMIT) {
            log.warn("limit 값이 최소값보다 작습니다: {}, 최소값: {}", limit, MIN_POPULAR_MUSIC_LIMIT);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "limit은 " + MIN_POPULAR_MUSIC_LIMIT + " 이상이어야 합니다", 
                               "providedLimit", limit, 
                               "minLimit", MIN_POPULAR_MUSIC_LIMIT,
                               "maxLimit", MAX_POPULAR_MUSIC_LIMIT));
        }
        
        if (limit > MAX_POPULAR_MUSIC_LIMIT) {
            log.warn("limit 값이 최대값을 초과합니다: {}, 최대값: {}", limit, MAX_POPULAR_MUSIC_LIMIT);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "limit은 " + MAX_POPULAR_MUSIC_LIMIT + " 이하여야 합니다", 
                               "providedLimit", limit, 
                               "minLimit", MIN_POPULAR_MUSIC_LIMIT,
                               "maxLimit", MAX_POPULAR_MUSIC_LIMIT));
        }
        
        List<MusicResponse> musicList = musicService.getPopularMusicByCategory(categoryId, limit);
        
        log.info("카테고리별 인기 음악 조회 완료, categoryId: {}, limit: {}, 결과 개수: {}", 
                categoryId, limit, musicList.size());
        
        return ResponseEntity.ok(musicList);
    }

    /**
     * 음악 재생수 증가
     * 
     * 사용자별 재생 로그를 기록하고 음악의 전체 재생수를 원자적으로 증가시킵니다.
     * 
     * @param musicId 재생할 음악 ID
     * @return 성공 메시지
     * @throws IllegalArgumentException 음악을 찾을 수 없는 경우
     */
    @PostMapping("/{musicId}/play")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> incrementPlayCount(@PathVariable UUID musicId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("음악 재생수 증가 요청, musicId: {}, userId: {}", musicId, userId);
        
        try {
            musicService.incrementPlayCount(musicId, userId);
            
            log.info("음악 재생수 증가 완료, musicId: {}, userId: {}", musicId, userId);
            
            return ResponseEntity.ok(Map.of(
                    "message", "재생수가 증가되었습니다",
                    "musicId", musicId,
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("음악 재생수 증가 실패, musicId: {}, userId: {}, error: {}", musicId, userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(),
                               "musicId", musicId));
        }
    }
} 