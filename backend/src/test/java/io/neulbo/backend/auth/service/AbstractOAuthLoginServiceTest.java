package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AbstractOAuthLoginServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    private TestOAuthLoginService oAuthLoginService;

    @BeforeEach
    void setUp() {
        oAuthLoginService = new TestOAuthLoginService(webClient, userRepository, jwtProvider);
    }

    @Test
    @DisplayName("사용자 역할이 null인 경우 기본 USER 역할 사용")
    void shouldUseDefaultUserRole_WhenUserRoleIsNull() throws Exception {
        // given
        String role = null;

        // when
        List<String> result = invokeCreateSafeRolesList(role);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("USER");
    }

    @Test
    @DisplayName("사용자 역할이 빈 문자열인 경우 기본 USER 역할 사용")
    void shouldUseDefaultUserRole_WhenUserRoleIsEmpty() throws Exception {
        // given
        String role = "";

        // when
        List<String> result = invokeCreateSafeRolesList(role);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("USER");
    }

    @Test
    @DisplayName("사용자 역할이 공백만 있는 경우 기본 USER 역할 사용")
    void shouldUseDefaultUserRole_WhenUserRoleIsWhitespace() throws Exception {
        // given
        String role = "   ";

        // when
        List<String> result = invokeCreateSafeRolesList(role);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("USER");
    }

    @Test
    @DisplayName("사용자 역할이 유효한 경우 해당 역할 사용")
    void shouldUseActualRole_WhenUserRoleIsValid() throws Exception {
        // given
        String role = "ADMIN";

        // when
        List<String> result = invokeCreateSafeRolesList(role);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("사용자 역할에 앞뒤 공백이 있는 경우 trim 처리")
    void shouldTrimUserRole_WhenRoleHasWhitespace() throws Exception {
        // given
        String role = "  MODERATOR  ";

        // when
        List<String> result = invokeCreateSafeRolesList(role);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("MODERATOR");
    }

    /**
     * 리플렉션을 사용하여 private 메서드 createSafeRolesList를 호출합니다.
     */
    @SuppressWarnings("unchecked")
    private List<String> invokeCreateSafeRolesList(String role) throws Exception {
        Method method = AbstractOAuthLoginService.class.getDeclaredMethod("createSafeRolesList", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(oAuthLoginService, role);
    }

    /**
     * 테스트를 위한 AbstractOAuthLoginService 구현체
     */
    private static class TestOAuthLoginService extends AbstractOAuthLoginService {

        public TestOAuthLoginService(WebClient webClient, UserRepository userRepository, JwtProvider jwtProvider) {
            super(webClient, userRepository, jwtProvider);
        }

        @Override
        public OAuthToken getToken(String code) {
            return new OAuthToken("access-token", "Bearer", "refresh-token", 3600);
        }

        @Override
        public OAuthUser getUserInfo(String accessToken) {
            return new OAuthUser("12345", "test@example.com", "Test User", "https://example.com/profile.jpg");
        }

        @Override
        public Mono<OAuthToken> getTokenReactive(String code) {
            return Mono.just(new OAuthToken("access-token", "Bearer", "refresh-token", 3600));
        }

        @Override
        public Mono<OAuthUser> getUserInfoReactive(String accessToken) {
            return Mono.just(new OAuthUser("12345", "test@example.com", "Test User", "https://example.com/profile.jpg"));
        }
    }
} 