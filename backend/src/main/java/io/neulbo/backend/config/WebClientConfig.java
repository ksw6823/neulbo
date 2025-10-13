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
 * 
 * @deprecated Authorization Code 방식은 더 이상 사용되지 않습니다. 
 *             새로운 OAuth 시스템은 외부 HTTP 호출이 필요하지 않습니다.
 */
@Deprecated(since = "2025-10-12", forRemoval = true)
@Configuration
public class WebClientConfig {
    
    /**
     * OAuth 서비스용 최적화된 WebClient 빈
     * 
     * @deprecated DirectOAuthLoginService는 WebClient가 필요하지 않습니다.
     */
    @Deprecated(since = "2025-10-12", forRemoval = true)
    @Bean
    public WebClient webClient() {
        // 연결 풀 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("oauth-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // HTTP 클라이언트 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024))
                .build();
    }
}