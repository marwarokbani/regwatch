package com.regwatch.llm;

/**
 * Port d'accès au LLM utilisé par tout le reste de l'application.
 * <p>
 * Le code métier (extraction d'exigences, jugement) ne dépend que de cette interface :
 * il ignore quel fournisseur répond et comment le fallback est organisé.
 */
public interface LlmClient {

    LlmResponse chat(LlmRequest request);
}
