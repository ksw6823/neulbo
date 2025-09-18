package io.neulbo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 버전 관리 설정
 * 모든 API 엔드포인트에 일관된 버전 prefix 적용
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /**
     * 전역 API 버전 prefix 설정
     * 모든 컨트롤러에 /api/v1 prefix가 자동으로 적용됩니다.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", 
            c -> c.getPackageName().contains("controller") && 
                 !c.getSimpleName().contains("Test")); // 테스트 컨트롤러 제외
    }
}

