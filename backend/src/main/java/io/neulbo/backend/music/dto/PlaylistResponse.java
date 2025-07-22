package io.neulbo.backend.music.dto;

import io.neulbo.backend.music.domain.Playlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistResponse {

    // 기본값 상수
    private static final String DEFAULT_UNKNOWN_USER = "알 수 없는 사용자";

    private UUID id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private Boolean isPublic;
    private Boolean isDefault;
    private Integer totalDurationSeconds;
    private Integer musicCount;
    private UUID userId;
    private String userName;
    private List<MusicResponse> musicList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 메서드 (음악 목록 포함)
     * 
     * Null Safety 처리:
     * - playlist.getUser()가 null인 경우: userId는 null, userName은 "알 수 없는 사용자"로 설정
     * - playlist.getUser().getUsername()이 null인 경우: "알 수 없는 사용자"로 설정
     * - playlist.getPlaylistMusicList()가 null인 경우: 빈 리스트로 설정
     * - PlaylistMusic 또는 Music이 null인 항목들은 필터링하여 제외
     * 
     * @param playlist 변환할 플레이리스트 엔티티
     * @return PlaylistResponse DTO (음악 목록 포함)
     * @throws IllegalArgumentException playlist가 null인 경우
     */
    public static PlaylistResponse from(Playlist playlist) {
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist는 null일 수 없습니다.");
        }
        // null safety 체크 및 기본값 설정
        List<MusicResponse> musicList = new ArrayList<>();
        if (playlist.getPlaylistMusicList() != null) {
            musicList = playlist.getPlaylistMusicList().stream()
                    .filter(pm -> pm != null && pm.getMusic() != null) // null PlaylistMusic 또는 Music 필터링
                    .map(pm -> MusicResponse.fromWithoutCategory(pm.getMusic()))
                    .collect(Collectors.toList());
        }

        // User 정보 null safety 처리
        UUID userId = null;
        String userName = DEFAULT_UNKNOWN_USER;
        if (playlist.getUser() != null) {
            userId = playlist.getUser().getId();
            userName = playlist.getUser().getUsername() != null ? 
                    playlist.getUser().getUsername() : DEFAULT_UNKNOWN_USER;
        }

        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .isPublic(playlist.getIsPublic())
                .isDefault(playlist.getIsDefault())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .musicCount(playlist.getMusicCount())
                .userId(userId)
                .userName(userName)
                .musicList(musicList)
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 메서드 (음악 목록 제외, 성능 최적화용)
     * 
     * Null Safety 처리:
     * - playlist.getUser()가 null인 경우: userId는 null, userName은 "알 수 없는 사용자"로 설정
     * - playlist.getUser().getUsername()이 null인 경우: "알 수 없는 사용자"로 설정
     * - musicList는 항상 빈 ArrayList로 설정
     * 
     * @param playlist 변환할 플레이리스트 엔티티
     * @return PlaylistResponse DTO (빈 음악 목록 포함)
     * @throws IllegalArgumentException playlist가 null인 경우
     */
    public static PlaylistResponse fromWithoutMusicList(Playlist playlist) {
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist는 null일 수 없습니다.");
        }
        // User 정보 null safety 처리
        UUID userId = null;
        String userName = DEFAULT_UNKNOWN_USER;
        if (playlist.getUser() != null) {
            userId = playlist.getUser().getId();
            userName = playlist.getUser().getUsername() != null ? 
                    playlist.getUser().getUsername() : DEFAULT_UNKNOWN_USER;
        }

        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .isPublic(playlist.getIsPublic())
                .isDefault(playlist.getIsDefault())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .musicCount(playlist.getMusicCount())
                .userId(userId)
                .userName(userName)
                .musicList(new ArrayList<>())
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }

    // 총 재생시간을 시:분:초 형태로 포맷팅하는 메서드
    public String getFormattedTotalDuration() {
        if (totalDurationSeconds == null || totalDurationSeconds <= 0) {
            return "00:00:00";
        }
        
        int hours = totalDurationSeconds / 3600;
        int minutes = (totalDurationSeconds % 3600) / 60;
        int seconds = totalDurationSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
} 