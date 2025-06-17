package io.neulbo.backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.neulbo.backend.auth.dto.CustomUserDetails;
import io.neulbo.backend.auth.service.TokenBlacklistService;
import io.neulbo.backend.user.domain.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        Long userId = 1L;
        List<String> roles = List.of("USER");
        
        when(request.getHeader("Authorization")).thenReturn("bearer " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtProvider.getRolesFromToken(token)).thenReturn(roles);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(jwtProvider).getRolesFromToken(token);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext 검증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assertThat(userDetails.getUserId()).isEqualTo(userId);
        assertThat(userDetails.getProvider()).isEqualTo("unknown");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("BEARER 대문자로 된 접두사 처리")
    void shouldProcessToken_WhenBearerIsUpperCase() throws Exception {
        // given
        String token = "valid.jwt.token";
        Long userId = 1L;
        List<String> roles = List.of("USER");
        
        when(request.getHeader("Authorization")).thenReturn("BEARER " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtProvider.getRolesFromToken(token)).thenReturn(roles);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(jwtProvider).getRolesFromToken(token);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext 검증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 Authorization 헤더 처리")
    void shouldProcessToken_WhenAuthHeaderHasWhitespace() throws Exception {
        // given
        String token = "valid.jwt.token";
        Long userId = 1L;
        List<String> roles = List.of("USER");
        
        when(request.getHeader("Authorization")).thenReturn("  Bearer " + token + "  ");
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtProvider.getRolesFromToken(token)).thenReturn(roles);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(jwtProvider).getRolesFromToken(token);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext 검증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("데이터베이스 권한이 JWT 토큰 권한보다 우선 적용됨")
    void shouldUseDatabaseRoleOverTokenRole() throws Exception {
        // given
        String token = "valid.jwt.token";
        Long userId = 1L;
        List<String> tokenRoles = List.of("USER");
        String dbRole = "ADMIN";
        
        User user = User.builder()
                .id(userId)
                .provider("google")
                .socialId("12345")
                .role(dbRole)
                .build();
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtProvider.getRolesFromToken(token)).thenReturn(tokenRoles);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(jwtProvider).getRolesFromToken(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext 검증 - 데이터베이스 권한이 적용되어야 함
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assertThat(userDetails.getUserId()).isEqualTo(userId);
        assertThat(userDetails.getProvider()).isEqualTo("google");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("사용자가 존재하지 않을 때 JWT 토큰 권한 사용")
    void shouldUseTokenRoleWhenUserNotFound() throws Exception {
        // given
        String token = "valid.jwt.token";
        Long userId = 1L;
        List<String> tokenRoles = List.of("USER");
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtProvider.getRolesFromToken(token)).thenReturn(tokenRoles);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verify(jwtProvider).getUserIdFromToken(token);
        verify(jwtProvider).getRolesFromToken(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext 검증 - JWT 토큰 권한이 적용되어야 함
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assertThat(userDetails.getUserId()).isEqualTo(userId);
        assertThat(userDetails.getProvider()).isEqualTo("unknown");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("블랙리스트된 토큰인 경우 SecurityContext 설정되지 않음")
    void shouldNotSetSecurityContext_WhenTokenIsBlacklisted() throws Exception {
        // given
        String token = "blacklisted.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistService.isBlacklisted(token)).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(blacklistService).isBlacklisted(token);
        verifyNoInteractions(jwtProvider, userRepository);
        verifyNoInteractions(filterChain); // 에러 응답으로 인해 필터 체인이 호출되지 않음
        
        // SecurityContext가 설정되지 않아야 함
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }
} 