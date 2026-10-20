package com.regwatch.llm;

/**
 * Réponse normalisée, quel que soit le fournisseur.
 * Le fournisseur et le modèle sont conservés pour la traçabilité (ils seront persistés avec chaque verdict).
 */
public record LlmResponse(
        String content,
        String provider,
        String model,
        long latencyMs,
        Integer promptTokens,
        Integer completionTokens) {
}
