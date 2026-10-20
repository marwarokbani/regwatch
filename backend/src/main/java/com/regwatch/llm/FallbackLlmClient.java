package com.regwatch.llm;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Essaie les fournisseurs dans l'ordre de la chaîne et renvoie la première réponse obtenue.
 * <p>
 * Toute exception d'un fournisseur (quota 429, 5xx, timeout, service absent…) fait passer
 * au suivant. Les relances <em>au sein</em> d'un fournisseur sont gérées en amont par son
 * RetryTemplate, volontairement court pour ne pas retarder la bascule.
 */
public class FallbackLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmClient.class);

    private final List<LlmProvider> providers;

    public FallbackLlmClient(List<LlmProvider> providers) {
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("La chaîne de fournisseurs LLM est vide");
        }
        this.providers = List.copyOf(providers);
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        List<String> failures = new ArrayList<>();
        for (LlmProvider provider : providers) {
            try {
                LlmResponse response = provider.chat(request);
                if (!failures.isEmpty()) {
                    log.info("Réponse obtenue via '{}' après échec de : {}", provider.name(), failures);
                }
                return response;
            } catch (RuntimeException e) {
                log.warn("Fournisseur LLM '{}' en échec : {}", provider.name(), e.getMessage());
                failures.add(provider.name() + " → " + e.getMessage());
            }
        }
        throw new LlmUnavailableException(failures);
    }

    public List<String> providerNames() {
        return providers.stream().map(p -> p.name() + " (" + p.model() + ")").toList();
    }
}
