package io.neulbo.backend.music.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Music> musicList = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void updateInfo(String name, String description, String iconUrl, String colorCode) {
        // name 유효성 검사 (필수 값)
        if (name == null) {
            throw new IllegalArgumentException("카테고리 이름은 null일 수 없습니다.");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름은 빈 문자열일 수 없습니다.");
        }
        if (name.trim().length() > 50) {
            throw new IllegalArgumentException("카테고리 이름은 50자를 초과할 수 없습니다.");
        }
        
        // description 유효성 검사 (선택 값, 빈 문자열은 null로 처리)
        String validDescription = (description != null && description.trim().isEmpty()) ? null : description;
        if (validDescription != null && validDescription.length() > 200) {
            throw new IllegalArgumentException("카테고리 설명은 200자를 초과할 수 없습니다.");
        }
        
        // iconUrl 유효성 검사 (선택 값, 빈 문자열은 null로 처리)
        String validIconUrl = (iconUrl != null && iconUrl.trim().isEmpty()) ? null : iconUrl;
        
        // colorCode 유효성 검사 (선택 값, 빈 문자열은 null로 처리)
        String validColorCode = (colorCode != null && colorCode.trim().isEmpty()) ? null : colorCode;
        if (validColorCode != null && !isValidColorCode(validColorCode)) {
            throw new IllegalArgumentException("올바르지 않은 색상 코드 형식입니다. #RRGGBB 형식을 사용해주세요.");
        }
        
        // 유효성 검사를 통과한 값들로 업데이트
        this.name = name.trim();
        this.description = validDescription;
        this.iconUrl = validIconUrl;
        this.colorCode = validColorCode;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 색상 코드 유효성 검사 (HEX 형식: #RRGGBB)
     */
    private boolean isValidColorCode(String colorCode) {
        if (colorCode == null) {
            return false;
        }
        // #으로 시작하고 6자리 16진수 문자열인지 확인
        return colorCode.matches("^#[0-9A-Fa-f]{6}$");
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
} 