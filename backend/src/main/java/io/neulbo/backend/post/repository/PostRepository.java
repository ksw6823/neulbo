package io.neulbo.backend.post.repository;

import io.neulbo.backend.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorId(UUID authorId);
    Optional<Post> findByAuthorUsername(String userName);
}
