package io.neulbo.backend.music.dto;

import io.neulbo.backend.music.domain.Category;
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
public class CategoryResponse {

    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private String colorCode;
    private Integer sortOrder;
    private Boolean isActive;
    private Long musicCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity -> DTO 변환 메서드
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .colorCode(category.getColorCode())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .musicCount((long) category.getMusicList().size())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    // 음악 개수를 별도로 설정하는 버전
    public static CategoryResponse from(Category category, Long musicCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .colorCode(category.getColorCode())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .musicCount(musicCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
} 