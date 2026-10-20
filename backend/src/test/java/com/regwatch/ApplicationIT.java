package com.regwatch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Démarre l'application complète contre un vrai Postgres.
 * Ollama et le reranker ne tournent pas ici : on vérifie donc les probes, qui n'en dépendent pas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "regwatch.llm.chain=ollama")
@Import(TestcontainersConfiguration.class)
class ApplicationIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void livenessAndReadinessAreUp() {
        assertThat(http.getForEntity("/health/liveness", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> readiness = http.getForEntity("/health/readiness", String.class);
        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).contains("\"db\"");
    }

    @Test
    void healthReportsExternalServices() {
        String body = http.getForObject("/health", String.class);

        assertThat(body).contains("\"ollama\"", "\"reranker\"", "\"db\"");
    }

    @Test
    void flywayEnabledPgvector() {
        String version = jdbc.sql("SELECT extversion FROM pg_extension WHERE extname = 'vector'")
                .query(String.class).single();

        assertThat(version).startsWith("0.8");
    }
}
