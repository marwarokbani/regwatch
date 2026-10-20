package com.regwatch.common.health;

import java.util.List;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regwatch.common.ServicesProperties;

/**
 * Vérifie que le serveur Ollama d'embeddings répond ET que le modèle attendu est téléchargé
 * (le service ollama-init le tire au premier démarrage, ce qui peut prendre quelques minutes).
 */
@Component("ollama")
class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient client;
    private final String model;

    OllamaHealthIndicator(ServicesProperties properties) {
        this.client = HealthChecks.restClient(properties.embedding().baseUrl());
        this.model = properties.embedding().model();
    }

    @Override
    public Health health() {
        try {
            TagsResponse tags = client.get().uri("/api/tags").retrieve().body(TagsResponse.class);
            List<String> names = tags == null || tags.models() == null
                    ? List.of()
                    : tags.models().stream().map(ModelTag::name).toList();
            // Ollama suffixe les noms par ":latest" quand aucun tag n'est précisé.
            boolean present = names.stream().anyMatch(n -> n.equals(model) || n.equals(model + ":latest"));
            Health.Builder builder = present ? Health.up() : Health.down().withDetail("reason", "modèle non téléchargé");
            return builder.withDetail("model", model).withDetail("available", names).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("model", model).build();
        }
    }

    record TagsResponse(List<ModelTag> models) {
    }

    record ModelTag(String name) {
    }
}
