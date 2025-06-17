package io.neulbo.backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.neulbo.backend.auth.service.TokenBlacklistService;
import io.neulbo.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;
    
    @Mock
    private TokenBlacklistService blacklistService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtProvider, blacklistService, userRepository, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 null인 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenAuthHeaderIsNull() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("Authorization 헤더가 빈 문자열인 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenAuthHeaderIsEmpty() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("Authorization 헤더가 공백만 있는 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenAuthHeaderIsWhitespace() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("   ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("Bearer 접두사 없는 헤더인 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenAuthHeaderWithoutBearer() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic token123");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("Bearer만 있고 토큰이 없는 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenBearerWithoutToken() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("Bearer 뒤에 공백만 있는 경우 필터 체인 계속 진행")
    void shouldContinueFilterChain_WhenBearerWithOnlySpaces() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer   ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProvider, blacklistService);
    }

    @Test
    @DisplayName("대소문자 구분 없이 bearer 접두사 처리")
    void shouldProcessToken_WhenBearerIsCaseInsensitive() throws Exception {
        // given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("bearer " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(1L);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("BEARER 대문자로 된 접두사 처리")
    void shouldProcessToken_WhenBearerIsUpperCase() throws Exception {
        // given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("BEARER " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(1L);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 Authorization 헤더 처리")
    void shouldProcessToken_WhenAuthHeaderHasWhitespace() throws Exception {
        // given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("  Bearer " + token + "  ");
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(1L);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(filterChain).doFilter(request, response);
    }
} 