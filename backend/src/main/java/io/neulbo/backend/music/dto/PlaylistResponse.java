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

    // Entity -> DTO 변환 메서드 (음악 목록 포함)
    public static PlaylistResponse from(Playlist playlist) {
        List<MusicResponse> musicList = playlist.getPlaylistMusicList().stream()
                .map(pm -> MusicResponse.fromWithoutCategory(pm.getMusic()))
                .collect(Collectors.toList());

        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .isPublic(playlist.getIsPublic())
                .isDefault(playlist.getIsDefault())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .musicCount(playlist.getMusicCount())
                .userId(playlist.getUser().getId())
                .userName(playlist.getUser().getUsername())
                .musicList(musicList)
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }

    // Entity -> DTO 변환 메서드 (음악 목록 제외, 성능 최적화용)
    public static PlaylistResponse fromWithoutMusicList(Playlist playlist) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .thumbnailUrl(playlist.getThumbnailUrl())
                .isPublic(playlist.getIsPublic())
                .isDefault(playlist.getIsDefault())
                .totalDurationSeconds(playlist.getTotalDurationSeconds())
                .musicCount(playlist.getMusicCount())
                .userId(playlist.getUser().getId())
                .userName(playlist.getUser().getUsername())
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