package io.neulbo.backend.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthTestController 보안 개선사항 검증 테스트
 */
class AuthTestControllerSecurityTest {

    /**
     * local 프로파일에서 AuthTestController가 활성화되는지 테스트
     */
    @SpringBootTest
    @ActiveProfiles("local")
    static class LocalProfileTest {
        
        @Autowired(required = false)
        private AuthTestController authTestController;

        @Test
        @DisplayName("local 프로파일에서 AuthTestController 빈이 존재한다")
        void shouldHaveAuthTestControllerBean() {
            assertThat(authTestController).isNotNull();
        }
    }

    /**
     * production 프로파일에서 AuthTestController가 비활성화되는지 테스트
     */
    @SpringBootTest
    @ActiveProfiles("production")
    static class ProductionProfileTest {
        
        @Autowired(required = false)
        private AuthTestController authTestController;

        @Test
        @DisplayName("production 프로파일에서 AuthTestController 빈이 존재하지 않는다")
        void shouldNotHaveAuthTestControllerBean() {
            assertThat(authTestController).isNull();
        }
    }

    /**
     * 기본 프로파일에서 AuthTestController가 비활성화되는지 테스트
     */
    @SpringBootTest
    static class DefaultProfileTest {
        
        @Autowired(required = false)
        private AuthTestController authTestController;

        @Test
        @DisplayName("기본 프로파일에서 AuthTestController 빈이 존재하지 않는다")
        void shouldNotHaveAuthTestControllerBeanInDefault() {
            assertThat(authTestController).isNull();
        }
    }
} 