package com.regwatch.llm;

import java.util.List;

/** Levée quand tous les fournisseurs de la chaîne ont échoué. */
public class LlmUnavailableException extends RuntimeException {

    private final List<String> failures;

    public LlmUnavailableException(List<String> failures) {
        super("Aucun fournisseur LLM disponible : " + String.join(" | ", failures));
        this.failures = List.copyOf(failures);
    }

    public List<String> failures() {
        return failures;
    }
}
