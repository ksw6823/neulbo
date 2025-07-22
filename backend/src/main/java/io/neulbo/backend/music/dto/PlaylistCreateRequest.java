package io.neulbo.backend.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistCreateRequest {

    @NotBlank(message = "플레이리스트 이름은 필수입니다")
    @Size(max = 100, message = "플레이리스트 이름은 100자 이하여야 합니다")
    private String name;

    @Size(max = 300, message = "설명은 300자 이하여야 합니다")
    private String description;

    @Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다")
    private String thumbnailUrl;

    private Boolean isPublic = false;
} 