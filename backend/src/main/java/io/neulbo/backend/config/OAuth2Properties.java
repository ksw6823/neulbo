package io.neulbo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * OAuth2 클라이언트 설정 프로퍼티
 * 
 * @deprecated Authorization Code 방식은 더 이상 사용되지 않습니다. 
 *             {@link io.neulbo.backend.auth.service.DirectOAuthLoginService}를 사용하세요.
 */
@Deprecated(since = "2025-10-12", forRemoval = true)
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public record OAuth2Properties(
        @NestedConfigurationProperty Registration registration,
        @NestedConfigurationProperty Provider provider
) {

    /**
     * OAuth2 클라이언트 등록 정보
     */
    public record Registration(
            @NestedConfigurationProperty ClientConfig google,
            @NestedConfigurationProperty ClientConfig kakao,
            @NestedConfigurationProperty ClientConfig naver
    ) {
        
        /**
         * 개별 OAuth2 클라이언트 설정
         */
        public record ClientConfig(
                String clientId,
                String clientSecret,
                String redirectUri
        ) {}
    }

    /**
     * OAuth2 제공자 정보
     */
    public record Provider(
            @NestedConfigurationProperty ProviderConfig google,
            @NestedConfigurationProperty ProviderConfig kakao,
            @NestedConfigurationProperty ProviderConfig naver
    ) {
        
        /**
         * 개별 OAuth2 제공자 설정
         */
        public record ProviderConfig(
                String tokenUri,
                String userInfoUri
        ) {}
    }
}