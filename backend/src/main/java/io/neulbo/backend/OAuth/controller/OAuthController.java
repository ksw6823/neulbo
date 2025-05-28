package io.neulbo.backend.OAuth.controller;

import io.neulbo.backend.OAuth.dto.OAuthCodeRequest;
import io.neulbo.backend.OAuth.dto.OAuthToken;
import io.neulbo.backend.OAuth.dto.OAuthUser;
import io.neulbo.backend.OAuth.service.OAuthLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final Map<String, OAuthLoginService> loginServices;

    @PostMapping("/login/{provider}")
    public ResponseEntity<?> login(
            @PathVariable String provider,
            @RequestBody OAuthCodeRequest request
    ) {
        OAuthLoginService service = loginServices.get(provider.toLowerCase());

        if (service == null) {
            return ResponseEntity.badRequest().body("지원하지 않는 로그인 방식입니다.");
        }

        OAuthToken token = service.getToken(request.code());
        OAuthUser user = service.getUserInfo(token.accessToken());

        // 여기에 DB 저장 및 JWT 발급 로직이 들어갈 수 있음

        return ResponseEntity.ok(user); // or return JWT
    }
}