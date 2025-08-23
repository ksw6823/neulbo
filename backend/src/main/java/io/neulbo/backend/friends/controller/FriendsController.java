package io.neulbo.backend.friends.controller;

import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class FriendsController {

    private final UserRepository userRepository;

    @GetMapping("/friends")
    public ResponseEntity<List<Map<String, Object>>> getFriends(@AuthenticationPrincipal OAuth2User oAuth2User, String provider) {

        if (oAuth2User == null) {
            return ResponseEntity.status(401).body(null);
        }

        String kakaoId = oAuth2User.getAttribute("id").toString();
        User me = userRepository.findByProviderIdAndProvider(kakaoId, provider)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        List<User> allUsers = userRepository.findAll();

        List<Map<String, Object>> recommendedFriends = allUsers.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getUsername());
            map.put("isMe", user.getId().equals(me.getId()));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(recommendedFriends);
    }
}