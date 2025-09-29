package io.neulbo.backend.auth.LLM;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class LLMController {

    private final LLMService llmService;

    @Autowired
    public LLMController(LLMService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/api/llm/query")
    public ResponseEntity<String> sendLlmQuery(@RequestBody LLMDTO llmdto) {
        String response = llmService.callLlmApi(llmdto.getPrompt());
        return ResponseEntity.ok(response);
    }
}