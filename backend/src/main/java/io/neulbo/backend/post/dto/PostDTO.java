package io.neulbo.backend.post.dto;

import io.neulbo.backend.post.domain.Post;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String createdAt;
    private boolean isAuthor;

    public PostDTO(Long id, String title, String content, boolean isAuthor) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isAuthor = isAuthor;
    }
}
