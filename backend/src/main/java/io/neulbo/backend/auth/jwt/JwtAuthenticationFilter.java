package io.neulbo.backend.auth.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.neulbo.backend.auth.dto.CustomUserDetails;
import io.neulbo.backend.auth.service.TokenBlacklistService;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.error.ErrorResponse;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService blacklistService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Authorization 헤더 검증 개선
        if (StringUtils.hasText(authHeader)) {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (blacklistService.isBlacklisted(token)) {
                sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
                return;
            }

            try {
                Long userId = jwtProvider.getUserIdFromToken(token);
                
                // 사용자 정보 조회 (선택적)
                User user = userRepository.findById(userId).orElse(null);
                String provider = user != null ? user.getProvider() : "unknown";
                
                // CustomUserDetails 생성 (기본 ROLE_USER 권한 포함)
                CustomUserDetails userDetails = new CustomUserDetails(userId, provider);
                
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JWTVerificationException e) {
                sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
                return;
            }
        }


        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 JWT 토큰을 안전하게 추출합니다.
     * 
     * @param authHeader Authorization 헤더 값
     * @return 유효한 토큰이면 토큰 문자열, 그렇지 않으면 null
     */
    private String extractTokenFromHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        
        // 대소문자 구분 없이 "Bearer " 접두사 확인
        String trimmedHeader = authHeader.trim();
        String bearerPrefix = "Bearer ";
        
        if (trimmedHeader.length() < bearerPrefix.length()) {
            return null;
        }
        
        // 대소문자 구분 없이 "Bearer " 접두사 확인
        if (!trimmedHeader.substring(0, bearerPrefix.length()).equalsIgnoreCase(bearerPrefix)) {
            return null;
        }
        
        // "Bearer " 이후의 토큰 부분 추출
        String token = trimmedHeader.substring(bearerPrefix.length()).trim();
        
        // 토큰이 비어있지 않은지 확인
        return StringUtils.hasText(token) ? token : null;
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
