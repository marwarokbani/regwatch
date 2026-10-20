package com.regwatch.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regwatch.llm.LlmProperties.Provider;
import com.regwatch.llm.LlmProperties.ProviderType;

/** Construire un ChatModel Spring AI ne fait aucun appel réseau : pas besoin de mock ici. */
class LlmProviderFactoryTest {

    private final LlmProviderFactory factory = new LlmProviderFactory();

    @Test
    void buildsProvidersInChainOrder() {
        LlmProperties props = properties(List.of("ollama", "groq"), "secret", "secret");

        List<LlmProvider> chain = factory.buildChain(props);

        assertThat(chain).extracting(LlmProvider::name).containsExactly("ollama", "groq");
        assertThat(chain).extracting(LlmProvider::model).containsExactly("qwen2.5:3b", "llama-3.3-70b-versatile");
    }

    @Test
    void skipsOpenAiCompatibleProviderWithoutApiKey() {
        LlmProperties props = properties(List.of("groq", "gemini", "ollama"), "", "secret");

        assertThat(factory.buildChain(props)).extracting(LlmProvider::name).containsExactly("gemini", "ollama");
    }

    @Test
    void failsOnUnknownProviderName() {
        LlmProperties props = properties(List.of("groq", "mistral"), "secret", "secret");

        assertThatThrownBy(() -> factory.buildChain(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mistral");
    }

    @Test
    void failsWhenNoProviderIsUsable() {
        LlmProperties props = properties(List.of("groq", "gemini"), null, " ");

        assertThatThrownBy(() -> factory.buildChain(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun fournisseur LLM utilisable");
    }

    private static LlmProperties properties(List<String> chain, String groqKey, String geminiKey) {
        Duration timeout = Duration.ofSeconds(30);
        return new LlmProperties(chain, 0.0, Map.of(
                "groq", new Provider(ProviderType.OPENAI_COMPATIBLE, "https://api.groq.com/openai", null,
                        groqKey, "llama-3.3-70b-versatile", timeout, 2),
                "gemini", new Provider(ProviderType.OPENAI_COMPATIBLE,
                        "https://generativelanguage.googleapis.com/v1beta/openai", "/chat/completions",
                        geminiKey, "gemini-2.5-flash", timeout, 2),
                "ollama", new Provider(ProviderType.OLLAMA, "http://localhost:11435", null,
                        null, "qwen2.5:3b", timeout, 1)));
    }
}
