package com.regwatch.common;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * URLs des services annexes lancés par Docker Compose.
 *
 * @param embedding serveur Ollama dédié aux embeddings (bge-m3)
 * @param reranker  micro-service FastAPI de reranking
 */
@Validated
@ConfigurationProperties("regwatch.services")
public record ServicesProperties(Embedding embedding, Reranker reranker) {

    public record Embedding(@NotBlank String baseUrl, @NotBlank String model) {
    }

    public record Reranker(@NotBlank String baseUrl) {
    }
}
