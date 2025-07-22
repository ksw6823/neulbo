package io.neulbo.backend.music.service;

import io.neulbo.backend.music.domain.Music;
import io.neulbo.backend.music.dto.MusicResponse;
import io.neulbo.backend.music.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicService {

    private final MusicRepository musicRepository;

    /**
     * 모든 활성화된 음악 조회 (페이지네이션)
     */
    public Page<MusicResponse> getAllMusic(Pageable pageable) {
        log.debug("모든 음악 조회, page: {}", pageable.getPageNumber());
        
        return musicRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(MusicResponse::from);
    }

    /**
     * 카테고리별 음악 조회
     */
    public Page<MusicResponse> getMusicByCategory(UUID categoryId, Pageable pageable) {
        log.debug("카테고리별 음악 조회, categoryId: {}, page: {}", categoryId, pageable.getPageNumber());
        
        return musicRepository.findByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(categoryId, pageable)
                .map(MusicResponse::from);
    }

    /**
     * 음악 검색 (제목 또는 아티스트)
     */
    public Page<MusicResponse> searchMusic(String keyword, Pageable pageable) {
        log.debug("음악 검색, keyword: {}, page: {}", keyword, pageable.getPageNumber());
        
        return musicRepository.searchByTitleOrArtist(keyword, pageable)
                .map(MusicResponse::from);
    }

    /**
     * 인기 음악 조회 (재생수 기준)
     */
    public Page<MusicResponse> getPopularMusic(Pageable pageable) {
        log.debug("인기 음악 조회, page: {}", pageable.getPageNumber());
        
        return musicRepository.findPopularMusic(pageable)
                .map(MusicResponse::from);
    }

    /**
     * 최신 음악 조회
     */
    public Page<MusicResponse> getLatestMusic(Pageable pageable) {
        log.debug("최신 음악 조회, page: {}", pageable.getPageNumber());
        
        return musicRepository.findLatestMusic(pageable)
                .map(MusicResponse::from);
    }

    /**
     * 무료 음악만 조회
     */
    public Page<MusicResponse> getFreeMusic(Pageable pageable) {
        log.debug("무료 음악 조회, page: {}", pageable.getPageNumber());
        
        return musicRepository.findByIsPremiumFalseAndIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(MusicResponse::from);
    }

    /**
     * 프리미엄 음악만 조회
     */
    public Page<MusicResponse> getPremiumMusic(Pageable pageable) {
        log.debug("프리미엄 음악 조회, page: {}", pageable.getPageNumber());
        
        return musicRepository.findByIsPremiumTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(MusicResponse::from);
    }

    /**
     * 특정 음악 상세 조회
     */
    public MusicResponse getMusicById(UUID musicId) {
        log.debug("음악 상세 조회, musicId: {}", musicId);
        
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new IllegalArgumentException("음악을 찾을 수 없습니다: " + musicId));
        
        if (!music.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 음악입니다: " + musicId);
        }
        
        return MusicResponse.from(music);
    }

    /**
     * 재생시간 범위로 음악 조회
     */
    public Page<MusicResponse> getMusicByDurationRange(Integer minDuration, Integer maxDuration, Pageable pageable) {
        log.debug("재생시간 범위 음악 조회, minDuration: {}, maxDuration: {}, page: {}", 
                 minDuration, maxDuration, pageable.getPageNumber());
        
        return musicRepository.findByDurationRange(minDuration, maxDuration, pageable)
                .map(MusicResponse::from);
    }

    /**
     * 카테고리별 인기 음악 조회 (상위 N개)
     * 
     * @param categoryId 카테고리 ID
     * @param limit 조회할 음악 개수
     * @return 재생수 기준 상위 N개 음악 응답 목록
     */
    public List<MusicResponse> getPopularMusicByCategory(UUID categoryId, int limit) {
        log.debug("카테고리별 인기 음악 조회, categoryId: {}, limit: {}", categoryId, limit);
        
        return musicRepository.findPopularMusicByCategory(categoryId, limit)
                .stream()
                .map(MusicResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 음악 재생수 증가
     */
    @Transactional
    public void incrementPlayCount(UUID musicId) {
        log.debug("음악 재생수 증가, musicId: {}", musicId);
        
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new IllegalArgumentException("음악을 찾을 수 없습니다: " + musicId));
        
        music.incrementPlayCount();
        musicRepository.save(music);
        
        log.info("음악 재생수 증가 완료, musicId: {}, newPlayCount: {}", musicId, music.getPlayCount());
    }
} 