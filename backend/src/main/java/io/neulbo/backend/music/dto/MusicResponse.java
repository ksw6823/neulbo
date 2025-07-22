package io.neulbo.backend.music.dto;

import io.neulbo.backend.music.domain.Music;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicResponse {

    private UUID id;
    private String title;
    private String artist;
    private String album;
    private Integer durationSeconds;
    private String fileUrl;
    private String thumbnailUrl;
    private String description;
    private Long playCount;
    private Boolean isPremium;
    private Boolean isActive;
    private CategoryResponse category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity -> DTO 변환 메서드
    public static MusicResponse from(Music music) {
        return MusicResponse.builder()
                .id(music.getId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .album(music.getAlbum())
                .durationSeconds(music.getDurationSeconds())
                .fileUrl(music.getFileUrl())
                .thumbnailUrl(music.getThumbnailUrl())
                .description(music.getDescription())
                .playCount(music.getPlayCount())
                .isPremium(music.getIsPremium())
                .isActive(music.getIsActive())
                .category(music.getCategory() != null ? CategoryResponse.from(music.getCategory()) : null)
                .createdAt(music.getCreatedAt())
                .updatedAt(music.getUpdatedAt())
                .build();
    }

    // 카테고리 정보 없이 간단하게 변환하는 버전
    public static MusicResponse fromWithoutCategory(Music music) {
        return MusicResponse.builder()
                .id(music.getId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .album(music.getAlbum())
                .durationSeconds(music.getDurationSeconds())
                .fileUrl(music.getFileUrl())
                .thumbnailUrl(music.getThumbnailUrl())
                .description(music.getDescription())
                .playCount(music.getPlayCount())
                .isPremium(music.getIsPremium())
                .isActive(music.getIsActive())
                .category(null)
                .createdAt(music.getCreatedAt())
                .updatedAt(music.getUpdatedAt())
                .build();
    }

    // 재생시간을 분:초 형태로 포맷팅하는 메서드
    public String getFormattedDuration() {
        if (durationSeconds == null || durationSeconds <= 0) {
            return "00:00";
        }
        
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
} 