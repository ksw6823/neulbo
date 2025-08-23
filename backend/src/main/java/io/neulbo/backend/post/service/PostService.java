package io.neulbo.backend.post.service;

import io.neulbo.backend.post.domain.Post;
import io.neulbo.backend.post.dto.PostDTO;
import io.neulbo.backend.post.repository.PostRepository;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // Post 객체를 PostDTO로 변환하는 헬퍼 메서드
    private PostDTO convertToPostDTO(Post post, boolean isAuthor) {
        if (post == null) {
            return null;
        }
        String authorName = (post.getAuthor() != null) ? post.getAuthor().getUsername() : "Unknown";
        return new PostDTO(post.getId(), post.getTitle(), post.getContent(), isAuthor);
    }

    public PostDTO createPost(PostDTO postDTO, OAuth2User oAuth2User, String provider) {
        User currentUser = currentUser(oAuth2User, provider);

        Post post = new Post();
        post.setAuthor(currentUser);
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());

        Post savedPost = postRepository.save(post);

        return new PostDTO(savedPost.getId(), savedPost.getTitle(), savedPost.getContent(), true);
    }

    public List<PostDTO> getPostList() {
        return postRepository.findAll().stream()
                .map(post -> convertToPostDTO(post, false))
                .collect(Collectors.toList());
    }

    public PostDTO getPost(Long postId, OAuth2User oAuth2User, String provider) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        boolean isAuthor = isAuthor(post, oAuth2User, provider);
        return convertToPostDTO(post, isAuthor);
    }

    public PostDTO updatePost(Long postId, PostDTO postDTO, OAuth2User oAuth2User, String provider) {
        User currentUser = currentUser(oAuth2User, provider);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!isAuthor(post, oAuth2User, provider)) { // isAuthor 재활용
            throw new SecurityException("수정 권한이 없습니다.");
        }

        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        Post updatedPost = postRepository.save(post);

        return convertToPostDTO(updatedPost, true);
    }

    public void deletePost(Long postId, OAuth2User oAuth2User, String provider) {
        User currentUser = currentUser(oAuth2User, provider);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!isAuthor(post, oAuth2User, provider)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    public boolean isAuthor(Post post, OAuth2User oAuth2User, String provider) {
        if (oAuth2User == null || post.getAuthor() == null) return false;
        User currentUser = currentUser(oAuth2User, provider);
        return currentUser != null && post.getAuthor().getId().equals(currentUser.getId());
    }

    public User currentUser(OAuth2User oAuth2User, String provider) {
        if (oAuth2User == null) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }
        String kakaoId = oAuth2User.getAttribute("id").toString();
        System.out.println("내 카카오 아이디: " + kakaoId);
        return userRepository.findByProviderIdAndProvider(kakaoId, provider)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
    }
}