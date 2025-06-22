package io.neulbo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * OAuth2 클라이언트 설정 프로퍼티
 * 
 * spring.security.oauth2.client 하위의 모든 OAuth2 관련 설정을 타입 안전하게 관리합니다.
 * 불변성을 보장하고 컴파일 타임 안전성을 제공하며 테스트 가능성을 향상시킵니다.
 */
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