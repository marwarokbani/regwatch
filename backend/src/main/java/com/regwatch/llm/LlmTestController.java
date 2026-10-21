package com.regwatch.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Appel réel au LLM avec un prompt libre, pour vérifier la chaîne de fallback.
 * <p>
 * Profil {@code dev} uniquement : ouvert à tous, cet endpoint consommerait le quota des clés API
 * et servirait de proxy LLM gratuit. Hors {@code dev}, le bean n'existe pas et la route répond 404.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/llm")
class LlmTestController {

    private static final String TEST_SYSTEM_PROMPT =
            "Tu es un assistant de test. Réponds en français, en une ou deux phrases.";

    private final FallbackLlmClient llmClient;

    LlmTestController(FallbackLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/test")
    LlmResponse test(@Valid @RequestBody TestRequest request) {
        return llmClient.chat(LlmRequest.of(TEST_SYSTEM_PROMPT, request.prompt()));
    }

    record TestRequest(@NotBlank @Size(max = 2000) String prompt) {
    }
}
