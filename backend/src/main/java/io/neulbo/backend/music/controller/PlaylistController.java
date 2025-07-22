package io.neulbo.backend.music.controller;

import io.neulbo.backend.auth.util.SecurityUtils;
import io.neulbo.backend.music.dto.PlaylistAddMusicRequest;
import io.neulbo.backend.music.dto.PlaylistCreateRequest;
import io.neulbo.backend.music.dto.PlaylistResponse;
import io.neulbo.backend.music.service.PlaylistService;
import jakarta.validation.Valid;
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
@RequestMapping("/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    /**
     * 내 플레이리스트 목록 조회
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlaylistResponse>> getMyPlaylists() {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("내 플레이리스트 목록 조회 요청, userId: {}", userId);
        
        List<PlaylistResponse> playlists = playlistService.getUserPlaylists(userId);
        
        return ResponseEntity.ok(playlists);
    }

    /**
     * 특정 플레이리스트 상세 조회
     */
    @GetMapping("/{playlistId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> getPlaylistById(@PathVariable UUID playlistId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트 상세 조회 요청, playlistId: {}, userId: {}", playlistId, userId);
        
        PlaylistResponse playlist = playlistService.getPlaylistById(playlistId, userId);
        
        return ResponseEntity.ok(playlist);
    }

    /**
     * 플레이리스트 생성
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody PlaylistCreateRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트 생성 요청, userId: {}, name: {}", userId, request.getName());
        
        PlaylistResponse playlist = playlistService.createPlaylist(userId, request);
        
        return ResponseEntity.ok(playlist);
    }

    /**
     * 플레이리스트 수정
     */
    @PutMapping("/{playlistId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlaylistResponse> updatePlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistCreateRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트 수정 요청, playlistId: {}, userId: {}", playlistId, userId);
        
        PlaylistResponse playlist = playlistService.updatePlaylist(playlistId, userId, request);
        
        return ResponseEntity.ok(playlist);
    }

    /**
     * 플레이리스트 삭제
     */
    @DeleteMapping("/{playlistId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deletePlaylist(@PathVariable UUID playlistId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트 삭제 요청, playlistId: {}, userId: {}", playlistId, userId);
        
        playlistService.deletePlaylist(playlistId, userId);
        
        return ResponseEntity.ok("플레이리스트가 삭제되었습니다");
    }

    /**
     * 플레이리스트에 음악 추가
     */
    @PostMapping("/{playlistId}/music")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addMusicToPlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistAddMusicRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트에 음악 추가 요청, playlistId: {}, musicId: {}, userId: {}", 
                playlistId, request.getMusicId(), userId);
        
        playlistService.addMusicToPlaylist(playlistId, userId, request);
        
        return ResponseEntity.ok("음악이 플레이리스트에 추가되었습니다");
    }

    /**
     * 플레이리스트에서 음악 제거
     */
    @DeleteMapping("/{playlistId}/music/{musicId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeMusicFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID musicId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("플레이리스트에서 음악 제거 요청, playlistId: {}, musicId: {}, userId: {}", 
                playlistId, musicId, userId);
        
        playlistService.removeMusicFromPlaylist(playlistId, musicId, userId);
        
        return ResponseEntity.ok("음악이 플레이리스트에서 제거되었습니다");
    }

    /**
     * 공개 플레이리스트 목록 조회
     */
    @GetMapping("/public")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<PlaylistResponse>> getPublicPlaylists(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("공개 플레이리스트 목록 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<PlaylistResponse> playlists = playlistService.getPublicPlaylists(pageable);
        
        return ResponseEntity.ok(playlists);
    }

    /**
     * 인기 플레이리스트 조회
     */
    @GetMapping("/popular")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<PlaylistResponse>> getPopularPlaylists(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("인기 플레이리스트 조회 요청, page: {}", pageable.getPageNumber());
        
        Page<PlaylistResponse> playlists = playlistService.getPopularPlaylists(pageable);
        
        return ResponseEntity.ok(playlists);
    }
} 