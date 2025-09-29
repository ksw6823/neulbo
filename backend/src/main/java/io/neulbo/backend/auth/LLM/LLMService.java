package io.neulbo.backend.auth.LLM;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class LLMService {

    private final WebClient webClient;

    public LLMService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://127.0.0.1:8080").build();
    }

    public String callLlmApi(String prompt) {

        String requestBody = "{\"model\": \"your-model\", \"prompt\": \"" + prompt + "\"}";

        return webClient.post()
                .uri("/v1/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer [API 키]") // LLM 서버에 인증이 필요한 경우
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 비동기 요청을 동기식으로 처리
    }
}