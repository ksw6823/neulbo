package io.neulbo.backend.music.domain;

import io.neulbo.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "total_duration_seconds")
    @Builder.Default
    private Integer totalDurationSeconds = 0;

    @Column(name = "music_count")
    @Builder.Default
    private Integer musicCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    @Builder.Default
    private List<PlaylistMusic> playlistMusicList = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void updateInfo(String name, String description, String thumbnailUrl) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void makePublic() {
        this.isPublic = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void makePrivate() {
        this.isPublic = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unsetAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStats(int musicCount, int totalDurationSeconds) {
        this.musicCount = musicCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.updatedAt = LocalDateTime.now();
    }

    public void addMusic(PlaylistMusic playlistMusic) {
        this.playlistMusicList.add(playlistMusic);
        updateStats();
    }

    public void removeMusic(PlaylistMusic playlistMusic) {
        this.playlistMusicList.remove(playlistMusic);
        updateStats();
    }

    private void updateStats() {
        this.musicCount = this.playlistMusicList.size();
        this.totalDurationSeconds = this.playlistMusicList.stream()
                .mapToInt(pm -> pm.getMusic().getDurationSeconds() != null ? pm.getMusic().getDurationSeconds() : 0)
                .sum();
        this.updatedAt = LocalDateTime.now();
    }
} 