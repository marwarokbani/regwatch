package com.regwatch.llm;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration des fournisseurs LLM (préfixe {@code regwatch.llm}).
 *
 * @param chain       ordre de fallback, ex. [groq, gemini, ollama] (variable LLM_CHAIN)
 * @param temperature température par défaut ; 0 pour des verdicts aussi reproductibles que possible
 * @param providers   fournisseurs déclarés, indexés par nom logique
 */
@Validated
@ConfigurationProperties("regwatch.llm")
public record LlmProperties(
        @NotEmpty List<String> chain,
        @DefaultValue("0.0") double temperature,
        @NotEmpty Map<String, @Valid Provider> providers) {

    public enum ProviderType {
        /** API au format OpenAI /chat/completions : Groq, Gemini (endpoint de compatibilité)… */
        OPENAI_COMPATIBLE,
        OLLAMA
    }

    /**
     * @param completionsPath chemin de l'endpoint chat ; null = valeur Spring AI par défaut (/v1/chat/completions)
     * @param apiKey          obligatoire pour OPENAI_COMPATIBLE ; si absente, le fournisseur est ignoré
     * @param maxAttempts     tentatives sur erreur transitoire (5xx, réseau) avant de passer au suivant
     */
    public record Provider(
            @NotNull ProviderType type,
            @NotBlank String baseUrl,
            String completionsPath,
            String apiKey,
            @NotBlank String model,
            @DefaultValue("60s") Duration timeout,
            @DefaultValue("2") int maxAttempts) {
    }
}
