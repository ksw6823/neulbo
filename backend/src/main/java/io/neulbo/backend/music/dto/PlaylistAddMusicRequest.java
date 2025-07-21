package io.neulbo.backend.music.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistAddMusicRequest {

    @NotNull(message = "음악 ID는 필수입니다")
    private UUID musicId;

    private Integer sortOrder;
}

// 다중 음악 추가를 위한 별도 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class PlaylistAddMultipleMusicRequest {

    @NotNull(message = "음악 ID 목록은 필수입니다")
    private List<UUID> musicIds;
} 