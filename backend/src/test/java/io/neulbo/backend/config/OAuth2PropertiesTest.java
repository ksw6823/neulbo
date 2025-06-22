package io.neulbo.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2PropertiesTest {

    @Test
    @DisplayName("OAuth2Properties가 설정 값을 올바르게 바인딩한다")
    void shouldBindPropertiesCorrectly() {
        // given
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("spring.security.oauth2.client.registration.google.client-id", "google-client-id");
        properties.put("spring.security.oauth2.client.registration.google.client-secret", "google-client-secret");
        properties.put("spring.security.oauth2.client.registration.google.redirect-uri", "http://localhost:8080/login/oauth2/code/google");
        properties.put("spring.security.oauth2.client.provider.google.token-uri", "https://oauth2.googleapis.com/token");
        properties.put("spring.security.oauth2.client.provider.google.user-info-uri", "https://www.googleapis.com/oauth2/v2/userinfo");
        
        properties.put("spring.security.oauth2.client.registration.kakao.client-id", "kakao-client-id");
        properties.put("spring.security.oauth2.client.registration.kakao.client-secret", "kakao-client-secret");
        properties.put("spring.security.oauth2.client.registration.kakao.redirect-uri", "http://localhost:8080/login/oauth2/code/kakao");
        properties.put("spring.security.oauth2.client.provider.kakao.token-uri", "https://kauth.kakao.com/oauth/token");
        properties.put("spring.security.oauth2.client.provider.kakao.user-info-uri", "https://kapi.kakao.com/v2/user/me");
        
        properties.put("spring.security.oauth2.client.registration.naver.client-id", "naver-client-id");
        properties.put("spring.security.oauth2.client.registration.naver.client-secret", "naver-client-secret");
        properties.put("spring.security.oauth2.client.registration.naver.redirect-uri", "http://localhost:8080/login/oauth2/code/naver");
        properties.put("spring.security.oauth2.client.provider.naver.token-uri", "https://nid.naver.com/oauth2.0/token");
        properties.put("spring.security.oauth2.client.provider.naver.user-info-uri", "https://openapi.naver.com/v1/nid/me");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        OAuth2Properties oAuth2Properties = binder.bind("spring.security.oauth2.client", OAuth2Properties.class).get();

        // then
        // Google 설정 검증
        assertThat(oAuth2Properties.registration().google().clientId()).isEqualTo("google-client-id");
        assertThat(oAuth2Properties.registration().google().clientSecret()).isEqualTo("google-client-secret");
        assertThat(oAuth2Properties.registration().google().redirectUri()).isEqualTo("http://localhost:8080/login/oauth2/code/google");
        assertThat(oAuth2Properties.provider().google().tokenUri()).isEqualTo("https://oauth2.googleapis.com/token");
        assertThat(oAuth2Properties.provider().google().userInfoUri()).isEqualTo("https://www.googleapis.com/oauth2/v2/userinfo");

        // Kakao 설정 검증
        assertThat(oAuth2Properties.registration().kakao().clientId()).isEqualTo("kakao-client-id");
        assertThat(oAuth2Properties.registration().kakao().clientSecret()).isEqualTo("kakao-client-secret");
        assertThat(oAuth2Properties.registration().kakao().redirectUri()).isEqualTo("http://localhost:8080/login/oauth2/code/kakao");
        assertThat(oAuth2Properties.provider().kakao().tokenUri()).isEqualTo("https://kauth.kakao.com/oauth/token");
        assertThat(oAuth2Properties.provider().kakao().userInfoUri()).isEqualTo("https://kapi.kakao.com/v2/user/me");

        // Naver 설정 검증
        assertThat(oAuth2Properties.registration().naver().clientId()).isEqualTo("naver-client-id");
        assertThat(oAuth2Properties.registration().naver().clientSecret()).isEqualTo("naver-client-secret");
        assertThat(oAuth2Properties.registration().naver().redirectUri()).isEqualTo("http://localhost:8080/login/oauth2/code/naver");
        assertThat(oAuth2Properties.provider().naver().tokenUri()).isEqualTo("https://nid.naver.com/oauth2.0/token");
        assertThat(oAuth2Properties.provider().naver().userInfoUri()).isEqualTo("https://openapi.naver.com/v1/nid/me");
    }

    @Test
    @DisplayName("OAuth2Properties는 불변 객체이다")
    void shouldBeImmutable() {
        // given
        OAuth2Properties.Registration.ClientConfig clientConfig = 
                new OAuth2Properties.Registration.ClientConfig("client-id", "client-secret", "redirect-uri");
        OAuth2Properties.Provider.ProviderConfig providerConfig = 
                new OAuth2Properties.Provider.ProviderConfig("token-uri", "user-info-uri");

        // when & then
        assertThat(clientConfig.clientId()).isEqualTo("client-id");
        assertThat(clientConfig.clientSecret()).isEqualTo("client-secret");
        assertThat(clientConfig.redirectUri()).isEqualTo("redirect-uri");
        
        assertThat(providerConfig.tokenUri()).isEqualTo("token-uri");
        assertThat(providerConfig.userInfoUri()).isEqualTo("user-info-uri");
    }

    @Test
    @DisplayName("OAuth2Properties record는 equals와 hashCode를 올바르게 구현한다")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // given
        OAuth2Properties.Registration.ClientConfig clientConfig1 = 
                new OAuth2Properties.Registration.ClientConfig("client-id", "client-secret", "redirect-uri");
        OAuth2Properties.Registration.ClientConfig clientConfig2 = 
                new OAuth2Properties.Registration.ClientConfig("client-id", "client-secret", "redirect-uri");
        OAuth2Properties.Registration.ClientConfig clientConfig3 = 
                new OAuth2Properties.Registration.ClientConfig("different-id", "client-secret", "redirect-uri");

        // when & then
        assertThat(clientConfig1).isEqualTo(clientConfig2);
        assertThat(clientConfig1).isNotEqualTo(clientConfig3);
        assertThat(clientConfig1.hashCode()).isEqualTo(clientConfig2.hashCode());
        assertThat(clientConfig1.hashCode()).isNotEqualTo(clientConfig3.hashCode());
    }
} 