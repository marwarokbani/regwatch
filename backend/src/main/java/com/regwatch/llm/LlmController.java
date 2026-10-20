package com.regwatch.llm;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de diagnostic : vérifier la chaîne configurée et faire un appel réel au LLM. */
@RestController
@RequestMapping("/api/llm")
class LlmController {

    private static final String TEST_SYSTEM_PROMPT =
            "Tu es un assistant de test. Réponds en français, en une ou deux phrases.";

    private final FallbackLlmClient llmClient;

    LlmController(FallbackLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/providers")
    List<String> providers() {
        return llmClient.providerNames();
    }

    @PostMapping("/test")
    LlmResponse test(@Valid @RequestBody TestRequest request) {
        return llmClient.chat(LlmRequest.of(TEST_SYSTEM_PROMPT, request.prompt()));
    }

    record TestRequest(@NotBlank @Size(max = 2000) String prompt) {
    }
}
