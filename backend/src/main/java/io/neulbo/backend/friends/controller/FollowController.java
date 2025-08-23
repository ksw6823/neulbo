package io.neulbo.backend.friends.controller;

import io.neulbo.backend.friends.dto.FriendsDTO;
import io.neulbo.backend.friends.service.FriendsService;
import io.neulbo.backend.post.service.PostService;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FriendsService friendsService;
    private final UserService userService;
    private final PostService postService;

    @PostMapping("/users/{userName}/follows")
    public ResponseEntity<FriendsDTO> followUser(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                 @PathVariable("userName") String toUserName,
                                                 @RequestParam String provider,
                                                 @RequestParam UserDetails userDetails) {

        User fromUser = postService.currentUser(oAuth2User, provider);
        User toUser = userService.findByUserName(toUserName);

        FriendsDTO result = friendsService.follow(fromUser, toUser, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // 팔로잉 목록 조회
    @GetMapping("/users/{userName}/following")
    public ResponseEntity<List<FriendsDTO>> getFollowingList(@PathVariable("userName") String userName) {
        User user = userService.findByUserName(userName);
        List<FriendsDTO> followingList = friendsService.followingList(user);
        return ResponseEntity.ok(followingList);
    }

    // 팔로워 목록 조회
    @GetMapping("/users/{userName}/follower")
    public ResponseEntity<List<FriendsDTO>> getFollowerList(@PathVariable("userName") String userName) {
        User toUser = userService.findByUserName(userName);
        List<FriendsDTO> followerList = friendsService.followerList(toUser);
        return ResponseEntity.ok(followerList);
    }

    @DeleteMapping("/users/{userName}/follows")
    public ResponseEntity<Void> unfollowUser(@AuthenticationPrincipal OAuth2User oAuth2User,
                                             @PathVariable("userName") String toUserName,
                                             @RequestParam String provider) {

        User fromUser = postService.currentUser(oAuth2User, provider);
        User toUser = userService.findByUserName(toUserName);

        friendsService.cancelFollow(fromUser, toUser);
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}
