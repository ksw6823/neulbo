package io.neulbo.backend.music.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "playlist_music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlaylistMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "sort_order")
    private Integer sortOrder;

    // 생성 메서드
    public static PlaylistMusic create(Playlist playlist, Music music, Integer sortOrder) {
        return PlaylistMusic.builder()
                .playlist(playlist)
                .music(music)
                .sortOrder(sortOrder)
                .addedAt(LocalDateTime.now())
                .build();
    }

    // 비즈니스 메서드
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setMusic(Music music) {
        this.music = music;
    }
} 