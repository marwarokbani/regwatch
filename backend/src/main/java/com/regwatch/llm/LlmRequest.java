package com.regwatch.llm;

import org.springframework.util.Assert;

/**
 * @param systemPrompt consignes de rôle (peut être null)
 * @param userPrompt   contenu de la requête
 * @param temperature  null = température par défaut configurée
 */
public record LlmRequest(String systemPrompt, String userPrompt, Double temperature) {

    public LlmRequest {
        Assert.hasText(userPrompt, "userPrompt ne doit pas être vide");
    }

    public static LlmRequest of(String systemPrompt, String userPrompt) {
        return new LlmRequest(systemPrompt, userPrompt, null);
    }
}
