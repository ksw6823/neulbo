package io.neulbo.backend.music.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 플레이리스트와 음악 간의 다대다 관계를 나타내는 중간 엔티티
 * 
 * 캡슐화 개선사항:
 * - setter 메서드들을 protected로 제한하여 직접적인 관계 수정 방지
 * - 비즈니스 로직을 담은 의미 있는 메서드명 제공 (moveToPlaylist, changeMusic)
 * - 입력값 유효성 검사를 통한 데이터 무결성 보장
 * - static factory method(create)를 통한 객체 생성 권장
 */
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

    /**
     * 다른 플레이리스트로 이동
     * 
     * 주의: 양방향 관계 동기화는 서비스 레이어에서 처리해야 합니다.
     * 
     * @param newPlaylist 이동할 플레이리스트
     * @throws IllegalArgumentException newPlaylist가 null인 경우
     */
    public void moveToPlaylist(Playlist newPlaylist) {
        if (newPlaylist == null) {
            throw new IllegalArgumentException("플레이리스트는 null일 수 없습니다.");
        }
        this.playlist = newPlaylist;
    }

    /**
     * 음악 변경 (같은 플레이리스트 내에서 다른 음악으로 교체)
     * 
     * 주의: 양방향 관계 동기화는 서비스 레이어에서 처리해야 합니다.
     * 
     * @param newMusic 새로운 음악
     * @throws IllegalArgumentException newMusic이 null인 경우
     */
    public void changeMusic(Music newMusic) {
        if (newMusic == null) {
            throw new IllegalArgumentException("음악은 null일 수 없습니다.");
        }
        this.music = newMusic;
    }

    /**
     * 플레이리스트와 음악 관계의 유효성 검사
     * 
     * @return 유효한 관계인 경우 true, 그렇지 않으면 false
     */
    public boolean isValidRelationship() {
        return this.playlist != null && this.music != null;
    }

    /**
     * 특정 플레이리스트에 속하는지 확인
     * 
     * @param targetPlaylist 확인할 플레이리스트
     * @return 해당 플레이리스트에 속하면 true, 그렇지 않으면 false
     */
    public boolean belongsToPlaylist(Playlist targetPlaylist) {
        return this.playlist != null && this.playlist.equals(targetPlaylist);
    }

    /**
     * 특정 음악에 해당하는지 확인
     * 
     * @param targetMusic 확인할 음악
     * @return 해당 음악이면 true, 그렇지 않으면 false
     */
    public boolean containsMusic(Music targetMusic) {
        return this.music != null && this.music.equals(targetMusic);
    }
    
    // JPA 및 프레임워크용 setter (캡슐화를 위해 protected로 제한)
    protected void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    protected void setMusic(Music music) {
        this.music = music;
    }
} 