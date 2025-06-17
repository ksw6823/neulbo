package io.neulbo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 설정 클래스
 * OAuth 서비스의 리액티브 HTTP 통신을 위한 최적화된 WebClient 구성
 */
@Configuration
public class WebClientConfig {
    
    /**
     * OAuth 서비스용 최적화된 WebClient 빈
     * - 연결 풀 관리로 성능 최적화
     * - 타임아웃 설정으로 무한 대기 방지
     * - 리액티브 스트림 백프레셔 지원
     */
    @Bean
    public WebClient webClient() {
        // 연결 풀 설정 - OAuth 서비스 호출 최적화
        ConnectionProvider connectionProvider = ConnectionProvider.builder("oauth-pool")
                .maxConnections(50)                    // 최대 연결 수
                .maxIdleTime(Duration.ofSeconds(30))   // 유휴 연결 유지 시간
                .maxLifeTime(Duration.ofMinutes(5))    // 연결 최대 생존 시간
                .pendingAcquireTimeout(Duration.ofSeconds(10)) // 연결 대기 타임아웃
                .evictInBackground(Duration.ofSeconds(120))    // 백그라운드 정리 주기
                .build();

        // HTTP 클라이언트 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃 5초
                .responseTimeout(Duration.ofSeconds(10))             // 응답 타임아웃 10초
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))  // 읽기 타임아웃
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)) // 쓰기 타임아웃
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB 메모리 버퍼 제한
                .build();
    }
}