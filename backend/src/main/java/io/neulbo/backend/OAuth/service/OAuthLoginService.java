package io.neulbo.backend.OAuth.service;

import io.neulbo.backend.OAuth.dto.OAuthToken;
import io.neulbo.backend.OAuth.dto.OAuthUser;

public interface OAuthLoginService {
    OAuthToken getToken(String code);
    OAuthUser getUserInfo(String accessToken);
}
