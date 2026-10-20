package com.regwatch.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class FallbackLlmClientTest {

    private static final LlmRequest REQUEST = LlmRequest.of(null, "Bonjour");

    @Test
    void usesFirstProviderWhenItSucceeds() {
        LlmProvider groq = provider("groq");
        LlmProvider gemini = provider("gemini");
        when(groq.chat(any())).thenReturn(response("groq"));

        LlmResponse response = new FallbackLlmClient(List.of(groq, gemini)).chat(REQUEST);

        assertThat(response.provider()).isEqualTo("groq");
        verify(gemini, never()).chat(any());
    }

    @Test
    void fallsBackInChainOrderWhenProvidersFail() {
        LlmProvider groq = provider("groq");
        LlmProvider gemini = provider("gemini");
        LlmProvider ollama = provider("ollama");
        when(groq.chat(any())).thenThrow(new RuntimeException("429 - rate limit"));
        when(gemini.chat(any())).thenThrow(new RuntimeException("503"));
        when(ollama.chat(any())).thenReturn(response("ollama"));

        LlmResponse response = new FallbackLlmClient(List.of(groq, gemini, ollama)).chat(REQUEST);

        assertThat(response.provider()).isEqualTo("ollama");
    }

    @Test
    void reportsEveryFailureWhenWholeChainFails() {
        LlmProvider groq = provider("groq");
        LlmProvider ollama = provider("ollama");
        when(groq.chat(any())).thenThrow(new RuntimeException("429 - rate limit"));
        when(ollama.chat(any())).thenThrow(new RuntimeException("Connection refused"));

        FallbackLlmClient client = new FallbackLlmClient(List.of(groq, ollama));

        assertThatThrownBy(() -> client.chat(REQUEST))
                .isInstanceOf(LlmUnavailableException.class)
                .satisfies(e -> assertThat(((LlmUnavailableException) e).failures())
                        .containsExactly("groq → 429 - rate limit", "ollama → Connection refused"));
    }

    @Test
    void rejectsEmptyChain() {
        assertThatThrownBy(() -> new FallbackLlmClient(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static LlmProvider provider(String name) {
        LlmProvider provider = mock(LlmProvider.class);
        when(provider.name()).thenReturn(name);
        return provider;
    }

    private static LlmResponse response(String provider) {
        return new LlmResponse("ok", provider, "model", 10, 5, 2);
    }
}
