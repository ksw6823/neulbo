package io.neulbo.backend.music.service;

import io.neulbo.backend.music.domain.Music;
import io.neulbo.backend.music.domain.Playlist;
import io.neulbo.backend.music.domain.PlaylistMusic;
import io.neulbo.backend.music.dto.PlaylistAddMusicRequest;
import io.neulbo.backend.music.dto.PlaylistCreateRequest;
import io.neulbo.backend.music.dto.PlaylistResponse;
import io.neulbo.backend.music.repository.MusicRepository;
import io.neulbo.backend.music.repository.PlaylistMusicRepository;
import io.neulbo.backend.music.repository.PlaylistRepository;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 플레이리스트 서비스
 * 
 * 주요 기능:
 * - 플레이리스트 생성, 조회, 수정, 삭제
 * - 플레이리스트에 음악 추가/제거 시 통계 자동 업데이트
 *   (음악 개수, 총 재생시간이 실시간으로 동기화됨)
 * - 사용자별 플레이리스트 관리
 * - 공개/비공개 플레이리스트 지원
 * 
 * 통계 업데이트:
 * - addMusicToPlaylist(): 음악 추가 시 Playlist.addMusic() 호출
 * - removeMusicFromPlaylist(): 음악 제거 시 Playlist.removeMusic() 호출
 * - 두 메서드 모두 플레이리스트의 musicCount, totalDurationSeconds 자동 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistMusicRepository playlistMusicRepository;
    private final MusicRepository musicRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 플레이리스트 목록 조회
     */
    public List<PlaylistResponse> getUserPlaylists(UUID userId) {
        log.debug("사용자 플레이리스트 목록 조회, userId: {}", userId);
        
        return playlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PlaylistResponse::fromWithoutMusicList)
                .collect(Collectors.toList());
    }

    /**
     * 플레이리스트 상세 조회
     */
    public PlaylistResponse getPlaylistById(UUID playlistId, UUID userId) {
        log.debug("플레이리스트 상세 조회, playlistId: {}, userId: {}", playlistId, userId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다: " + playlistId));
        
        // 공개 플레이리스트이거나 본인의 플레이리스트인 경우만 조회 가능
        if (!playlist.getIsPublic() && !playlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다");
        }
        
        return PlaylistResponse.from(playlist);
    }

    /**
     * 플레이리스트 생성
     */
    @Transactional
    public PlaylistResponse createPlaylist(UUID userId, PlaylistCreateRequest request) {
        log.debug("플레이리스트 생성, userId: {}, name: {}", userId, request.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 동일한 이름의 플레이리스트가 있는지 확인
        if (playlistRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("동일한 이름의 플레이리스트가 이미 존재합니다: " + request.getName());
        }
        
        Playlist playlist = Playlist.builder()
                .name(request.getName())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .isPublic(request.getIsPublic())
                .user(user)
                .build();
        
        playlist = playlistRepository.save(playlist);
        
        log.info("플레이리스트 생성 완료, playlistId: {}, name: {}", playlist.getId(), playlist.getName());
        return PlaylistResponse.fromWithoutMusicList(playlist);
    }

    /**
     * 플레이리스트 수정
     */
    @Transactional
    public PlaylistResponse updatePlaylist(UUID playlistId, UUID userId, PlaylistCreateRequest request) {
        log.debug("플레이리스트 수정, playlistId: {}, userId: {}", playlistId, userId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다: " + playlistId));
        
        // 본인의 플레이리스트인지 확인
        if (!playlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다");
        }
        
        // 이름이 변경되었고, 동일한 이름의 다른 플레이리스트가 있는지 확인
        if (!playlist.getName().equals(request.getName()) && 
            playlistRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("동일한 이름의 플레이리스트가 이미 존재합니다: " + request.getName());
        }
        
        playlist.updateInfo(request.getName(), request.getDescription(), request.getThumbnailUrl());
        
        if (request.getIsPublic()) {
            playlist.makePublic();
        } else {
            playlist.makePrivate();
        }
        
        playlist = playlistRepository.save(playlist);
        
        log.info("플레이리스트 수정 완료, playlistId: {}", playlistId);
        return PlaylistResponse.fromWithoutMusicList(playlist);
    }

    /**
     * 플레이리스트 삭제
     */
    @Transactional
    public void deletePlaylist(UUID playlistId, UUID userId) {
        log.debug("플레이리스트 삭제, playlistId: {}, userId: {}", playlistId, userId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다: " + playlistId));
        
        // 본인의 플레이리스트인지 확인
        if (!playlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다");
        }
        
        // 기본 플레이리스트는 삭제 불가
        if (playlist.getIsDefault()) {
            throw new IllegalArgumentException("기본 플레이리스트는 삭제할 수 없습니다");
        }
        
        playlistRepository.delete(playlist);
        
        log.info("플레이리스트 삭제 완료, playlistId: {}", playlistId);
    }

    /**
     * 플레이리스트에 음악 추가
     * 
     * 추가된 기능:
     * - 음악 추가 후 플레이리스트 통계 자동 업데이트 (음악 개수, 총 재생시간)
     * - 중복 음악 추가 방지
     * - 정렬 순서 자동 할당 (미지정 시)
     * 
     * @param playlistId 플레이리스트 ID
     * @param userId 사용자 ID (권한 검증용)
     * @param request 음악 추가 요청 정보
     * @throws IllegalArgumentException 플레이리스트/음악 없음, 권한 없음, 중복 음악, 비활성 음악
     */
    @Transactional
    public void addMusicToPlaylist(UUID playlistId, UUID userId, PlaylistAddMusicRequest request) {
        log.debug("플레이리스트에 음악 추가, playlistId: {}, musicId: {}, userId: {}", 
                 playlistId, request.getMusicId(), userId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다: " + playlistId));
        
        // 본인의 플레이리스트인지 확인
        if (!playlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다");
        }
        
        Music music = musicRepository.findById(request.getMusicId())
                .orElseThrow(() -> new IllegalArgumentException("음악을 찾을 수 없습니다: " + request.getMusicId()));
        
        if (!music.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 음악입니다");
        }
        
        // 이미 플레이리스트에 존재하는지 확인
        if (playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, request.getMusicId())) {
            throw new IllegalArgumentException("이미 플레이리스트에 존재하는 음악입니다");
        }
        
        // 정렬 순서 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = playlistMusicRepository.findMaxSortOrderByPlaylistId(playlistId)
                    .orElse(0) + 1;
        }
        
        // 추가 전 통계 정보 로깅
        int oldMusicCount = playlist.getMusicCount();
        int oldTotalDuration = playlist.getTotalDurationSeconds();
        
        PlaylistMusic playlistMusic = PlaylistMusic.create(playlist, music, sortOrder);
        playlistMusicRepository.save(playlistMusic);
        
        // 플레이리스트에 음악 추가 및 통계 업데이트 (음악 개수, 총 재생시간)
        playlist.addMusic(playlistMusic);
        playlistRepository.save(playlist);
        
        // 통계 업데이트 확인 로깅
        log.info("플레이리스트에 음악 추가 완료, playlistId: {}, musicId: {}, " +
                "음악 개수: {} → {}, 총 재생시간: {}초 → {}초", 
                playlistId, request.getMusicId(), oldMusicCount, playlist.getMusicCount(), 
                oldTotalDuration, playlist.getTotalDurationSeconds());
    }

    /**
     * 플레이리스트에서 음악 제거
     * 
     * 추가된 기능:
     * - 음악 제거 후 플레이리스트 통계 자동 업데이트 (음악 개수, 총 재생시간)
     * - 존재하지 않는 음악 제거 시 예외 처리
     * 
     * @param playlistId 플레이리스트 ID
     * @param musicId 제거할 음악 ID
     * @param userId 사용자 ID (권한 검증용)
     * @throws IllegalArgumentException 플레이리스트/음악 없음, 권한 없음
     */
    @Transactional
    public void removeMusicFromPlaylist(UUID playlistId, UUID musicId, UUID userId) {
        log.debug("플레이리스트에서 음악 제거, playlistId: {}, musicId: {}, userId: {}", playlistId, musicId, userId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다: " + playlistId));
        
        // 본인의 플레이리스트인지 확인
        if (!playlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다");
        }
        
        PlaylistMusic playlistMusic = playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트에 해당 음악이 존재하지 않습니다"));
        
        // 삭제 전 통계 정보 로깅
        int oldMusicCount = playlist.getMusicCount();
        int oldTotalDuration = playlist.getTotalDurationSeconds();
        
        // 플레이리스트에서 음악 제거 및 통계 업데이트 (음악 개수, 총 재생시간)
        playlist.removeMusic(playlistMusic);
        playlistMusicRepository.delete(playlistMusic);
        playlistRepository.save(playlist);
        
        // 통계 업데이트 확인 로깅
        log.info("플레이리스트에서 음악 제거 완료, playlistId: {}, musicId: {}, " +
                "음악 개수: {} → {}, 총 재생시간: {}초 → {}초", 
                playlistId, musicId, oldMusicCount, playlist.getMusicCount(), 
                oldTotalDuration, playlist.getTotalDurationSeconds());
    }

    /**
     * 공개 플레이리스트 목록 조회
     */
    public Page<PlaylistResponse> getPublicPlaylists(Pageable pageable) {
        log.debug("공개 플레이리스트 목록 조회, page: {}", pageable.getPageNumber());
        
        return playlistRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable)
                .map(PlaylistResponse::fromWithoutMusicList);
    }

    /**
     * 인기 플레이리스트 조회
     */
    public Page<PlaylistResponse> getPopularPlaylists(Pageable pageable) {
        log.debug("인기 플레이리스트 조회, page: {}", pageable.getPageNumber());
        
        return playlistRepository.findPopularPublicPlaylists(pageable)
                .map(PlaylistResponse::fromWithoutMusicList);
    }
} 