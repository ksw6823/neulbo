package io.neulbo.backend.music.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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