package io.neulbo.backend.post.controller;

import io.neulbo.backend.post.dto.PostDTO;
import io.neulbo.backend.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<List<PostDTO>> getPosts() {
        List<PostDTO> posts = postService.getPostList();
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/posts")
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO,
                                              @AuthenticationPrincipal OAuth2User oAuth2User,
                                              @RequestParam String provider) {
        PostDTO savedPost = postService.createPost(postDTO, oAuth2User, provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable Long postId,
                                           @AuthenticationPrincipal OAuth2User oAuth2User,
                                           @RequestParam(required = false) String provider) {
        PostDTO post = postService.getPost(postId, oAuth2User, provider);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId,
                                              @RequestBody PostDTO postDTO,
                                              @AuthenticationPrincipal OAuth2User oAuth2User,
                                              @RequestParam String provider) {
        PostDTO updated = postService.updatePost(postId, postDTO, oAuth2User, provider);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal OAuth2User oAuth2User,
                                           @RequestParam(required = false) String provider) {
        postService.deletePost(postId, oAuth2User, provider);
        return ResponseEntity.noContent().build();
    }
}