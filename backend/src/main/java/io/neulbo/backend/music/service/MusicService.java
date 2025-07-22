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
     * 음악 재생수 증가 (사용자별 재생 로그 포함)
     * 
     * 동시성 안전성을 위해 원자적 업데이트를 사용하고,
     * 사용자별 재생 기록을 로그로 남깁니다.
     * 
     * @param musicId 재생할 음악 ID
     * @param userId 재생하는 사용자 ID
     * @throws IllegalArgumentException 음악을 찾을 수 없는 경우
     */
    @Transactional
    public void incrementPlayCount(UUID musicId, UUID userId) {
        log.debug("음악 재생수 증가, musicId: {}, userId: {}", musicId, userId);
        
        // 음악 존재 여부 확인
        if (!musicRepository.existsById(musicId)) {
            throw new IllegalArgumentException("음악을 찾을 수 없습니다: " + musicId);
        }
        
        // 원자적으로 재생수 증가 (동시성 안전)
        int updatedRows = musicRepository.incrementPlayCountAtomically(musicId);
        
        if (updatedRows == 0) {
            throw new IllegalArgumentException("음악 재생수 업데이트에 실패했습니다: " + musicId);
        }
        
        // 사용자별 재생 로그 기록
        log.info("사용자 음악 재생 기록, musicId: {}, userId: {}, timestamp: {}", 
                musicId, userId, System.currentTimeMillis());
        
        log.info("음악 재생수 증가 완료, musicId: {}, userId: {}", musicId, userId);
    }
    
    /**
     * @deprecated userId가 없는 기존 메서드는 사용 중단됨.
     *             대신 {@link #incrementPlayCount(UUID, UUID)}를 사용하세요.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Transactional
    public void incrementPlayCount(UUID musicId) {
        log.warn("Deprecated incrementPlayCount(musicId) 메서드 호출됨. userId를 포함한 메서드 사용을 권장합니다.");
        // 임시로 null userId로 새 메서드 호출
        incrementPlayCount(musicId, null);
    }
} 