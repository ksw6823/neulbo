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

    /**
     * Entity -> DTO 변환 메서드 (성능 주의)
     * 
     * @param category 변환할 카테고리 엔티티
     * @return CategoryResponse DTO
     * 
     * @deprecated 이 메서드는 {@code category.getMusicList().size()} 호출로 인해
     *             지연 로딩(Lazy Loading)을 트리거하여 추가적인 데이터베이스 쿼리를 발생시킵니다.
     *             이는 N+1 문제를 야기할 수 있으며 성능에 악영향을 미칩니다.
     *             
     *             성능 개선을 위해 {@link #from(Category, Long)} 메서드를 사용하세요.
     *             음악 개수는 다음과 같이 별도로 조회하는 것이 권장됩니다:
     *             
     *             <pre>{@code
     *             // 권장 방법 1: Repository에서 COUNT 쿼리로 조회
     *             Long musicCount = musicRepository.countByCategoryIdAndIsActiveTrue(categoryId);
     *             CategoryResponse response = CategoryResponse.from(category, musicCount);
     *             
     *             // 권장 방법 2: JPQL JOIN FETCH 사용
     *             @Query("SELECT c FROM Category c LEFT JOIN FETCH c.musicList WHERE c.id = :id")
     *             Category findByIdWithMusic(@Param("id") UUID id);
     *             }</pre>
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .colorCode(category.getColorCode())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .musicCount((long) category.getMusicList().size()) // 지연 로딩 트리거 - 성능 문제 발생 가능
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 메서드 (성능 최적화 버전)
     * 
     * 이 메서드는 지연 로딩을 방지하고 성능을 최적화하기 위해
     * 음악 개수를 외부에서 전달받습니다.
     * 
     * @param category 변환할 카테고리 엔티티
     * @param musicCount 해당 카테고리의 음악 개수 (별도 COUNT 쿼리로 조회 권장)
     * @return CategoryResponse DTO
     * 
     * @since 1.0
     */
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