package com.regwatch.llm;

/**
 * Un fournisseur concret (Groq, Gemini, Ollama…). Les fournisseurs sont des maillons
 * de la chaîne de fallback ; le reste de l'application passe par {@link LlmClient}.
 */
public interface LlmProvider {

    /** Nom logique tel qu'il apparaît dans LLM_CHAIN (ex. "groq"). */
    String name();

    String model();

    LlmResponse chat(LlmRequest request);
}
