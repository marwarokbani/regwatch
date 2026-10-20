package com.regwatch.common.health;

import java.util.Map;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regwatch.common.ServicesProperties;

/** Le service de reranking renvoie 503 tant que son modèle est en cours de chargement. */
@Component("reranker")
class RerankerHealthIndicator implements HealthIndicator {

    private final RestClient client;

    RerankerHealthIndicator(ServicesProperties properties) {
        this.client = HealthChecks.restClient(properties.reranker().baseUrl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Health health() {
        try {
            Map<String, Object> body = client.get().uri("/health").retrieve().body(Map.class);
            return Health.up().withDetails(body == null ? Map.of() : body).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
