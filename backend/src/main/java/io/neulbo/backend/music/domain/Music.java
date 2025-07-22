package io.neulbo.backend.music.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Music {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String artist;

    @Column(length = 100)
    private String album;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String description;

    @Column(name = "play_count")
    @Builder.Default
    private Long playCount = 0L;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "music", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    @Builder.Default
    private List<PlaylistMusic> playlistMusicList = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void updateInfo(String title, String artist, String album, String description) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateUrls(String fileUrl, String thumbnailUrl) {
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCategory(Category category) {
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재생 횟수 증가 (비원자적 연산)
     * 
     * @deprecated 동시성 문제로 인해 사용 중단됨. 
     *             대신 MusicRepository.incrementPlayCountAtomically(UUID musicId)를 사용하세요.
     *             이 메서드는 race condition을 일으킬 수 있으므로 프로덕션 환경에서 사용하지 마세요.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public void incrementPlayCount() {
        this.playCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPremium(boolean isPremium) {
        this.isPremium = isPremium;
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