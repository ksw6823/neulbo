package io.neulbo.backend.config;

import io.neulbo.backend.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/oauth/login/**", "/token/**").permitAll() // OAuth 로그인 엔드포인트
                        .requestMatchers("/actuator/health").permitAll() // 헬스체크
                        .requestMatchers("/api/auth/test/**").authenticated() // 테스트 엔드포인트는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정 - Flutter 앱과의 통신을 위한 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정 (개발/프로덕션 환경별로 분리)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",           // Flutter 개발 서버
                "https://neulbo1.com",          // 프로덕션 도메인 (AWS Lightsail)
                "https://*.neulbo1.com",        // 서브도메인 지원
                "https://*.vercel.app",         // Vercel 배포 (필요시)
                "https://*.netlify.app"         // Netlify 배포 (필요시)
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", 
                "Accept", "Origin", "Access-Control-Request-Method", 
                "Access-Control-Request-Headers"
        ));
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        
        // 자격 증명 허용 (JWT 토큰 사용 시 필요)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * CORS 설정 헬퍼 메서드 (사용자 정의 설정 시 활용)
     */
    public void configureCors() {
        // 필요 시 추가적인 CORS 설정 로직
        // 예: 환경별 동적 Origin 설정 등
    }

    /**
     * CSRF 설정 헬퍼 메서드 (현재는 비활성화 상태)
     */
    public void configureCsrf() {
        // JWT 기반 인증에서는 CSRF 보호가 불필요하므로 비활성화
        // 필요 시 CSRF 토큰 기반 보호 로직 구현 가능
    }
}
