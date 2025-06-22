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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
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
                
                // JWT 토큰에서 권한 정보 추출 (우선순위 1)
                List<String> rolesFromToken = jwtProvider.getRolesFromToken(token);
                
                // 사용자 정보 조회하여 최신 권한 정보 확인 (우선순위 2)
                User user = userRepository.findById(userId).orElse(null);
                String provider = user != null ? user.getProvider() : "unknown";
                
                // 최종 권한 결정: 데이터베이스 권한이 있으면 우선 사용, 없으면 토큰 권한 사용
                List<String> finalRoles = determineFinalRoles(user, rolesFromToken);
                
                // CustomUserDetails 생성 (동적 권한 포함)
                CustomUserDetails userDetails = new CustomUserDetails(userId, provider, finalRoles);
                
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
     * 최종 사용자 역할을 결정합니다.
     * 
     * 우선순위:
     * 1. 데이터베이스의 사용자 역할 (최신 정보)
     * 2. JWT 토큰의 역할 정보
     * 3. 기본 역할 "USER" (모든 경우에 최소 하나의 역할 보장)
     * 
     * @param user 데이터베이스에서 조회한 사용자 정보 (null 가능)
     * @param rolesFromToken JWT 토큰에서 추출한 역할 리스트 (null 가능)
     * @return 최종 역할 리스트 (항상 최소 하나의 역할 포함)
     */
    private List<String> determineFinalRoles(User user, List<String> rolesFromToken) {
        // 1순위: 데이터베이스의 사용자 역할 확인
        if (user != null && user.getRole() != null && !user.getRole().trim().isEmpty()) {
            String dbRole = user.getRole().trim();
            log.debug("Using database role for user {}: {}", user.getId(), dbRole);
            return List.of(dbRole);
        }
        
        // 2순위: JWT 토큰의 역할 정보 확인
        if (rolesFromToken != null && !rolesFromToken.isEmpty()) {
            // 토큰의 역할들이 모두 유효한지 확인
            List<String> validRoles = rolesFromToken.stream()
                    .filter(role -> role != null && !role.trim().isEmpty())
                    .map(String::trim)
                    .toList();
            
            if (!validRoles.isEmpty()) {
                log.debug("Using token roles: {}", validRoles);
                return validRoles;
            } else {
                log.warn("Token contains invalid roles (null or empty): {}", rolesFromToken);
            }
        }
        
        // 3순위: 기본 역할 "USER" 제공 (권한 부여 실패 방지)
        log.info("No valid roles found, assigning default USER role. User: {}, Token roles: {}", 
                user != null ? user.getId() : "null", rolesFromToken);
        return List.of("USER");
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
