package com.regwatch.llm;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Diagnostic public : la chaîne de fournisseurs configurée (affichée par la page d'état du frontend). */
@RestController
@RequestMapping("/api/llm")
class LlmController {

    private final FallbackLlmClient llmClient;

    LlmController(FallbackLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/providers")
    List<String> providers() {
        return llmClient.providerNames();
    }
}
