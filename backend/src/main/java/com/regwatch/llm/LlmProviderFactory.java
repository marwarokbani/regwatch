package com.regwatch.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.regwatch.llm.LlmProperties.Provider;
import com.regwatch.llm.LlmProperties.ProviderType;

/**
 * Construit la chaîne de fournisseurs à partir de la configuration.
 * Ajouter un fournisseur compatible OpenAI (Mistral, OpenRouter…) ne demande que de la config.
 */
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    public List<LlmProvider> buildChain(LlmProperties properties) {
        List<LlmProvider> chain = new ArrayList<>();
        for (String name : properties.chain()) {
            Provider config = properties.providers().get(name);
            if (config == null) {
                throw new IllegalStateException("Fournisseur LLM inconnu dans la chaîne : '" + name
                        + "'. Déclarés : " + properties.providers().keySet());
            }
            if (config.type() == ProviderType.OPENAI_COMPATIBLE && !StringUtils.hasText(config.apiKey())) {
                // Clé absente = fournisseur non souhaité : on l'ignore plutôt que d'échouer au démarrage,
                // ce qui permet de lancer le projet avec une seule clé (ou aucune, avec Ollama).
                log.warn("Fournisseur LLM '{}' ignoré : aucune clé API configurée", name);
                continue;
            }
            chain.add(new ChatModelLlmProvider(name, config.model(), createChatModel(config, properties.temperature())));
        }
        if (chain.isEmpty()) {
            throw new IllegalStateException("Aucun fournisseur LLM utilisable. Renseignez au moins une clé API "
                    + "(GROQ_API_KEY, GEMINI_API_KEY) ou ajoutez 'ollama' à LLM_CHAIN.");
        }
        log.info("Chaîne LLM : {}", chain.stream().map(p -> p.name() + "(" + p.model() + ")").toList());
        return chain;
    }

    private ChatModel createChatModel(Provider config, double temperature) {
        return switch (config.type()) {
            case OPENAI_COMPATIBLE -> {
                OpenAiApi.Builder api = OpenAiApi.builder()
                        .baseUrl(config.baseUrl())
                        .apiKey(config.apiKey())
                        .restClientBuilder(restClientBuilder(config.timeout()));
                if (StringUtils.hasText(config.completionsPath())) {
                    api.completionsPath(config.completionsPath());
                }
                yield OpenAiChatModel.builder()
                        .openAiApi(api.build())
                        .defaultOptions(OpenAiChatOptions.builder()
                                .model(config.model())
                                .temperature(temperature)
                                .build())
                        .retryTemplate(retryTemplate(config.maxAttempts()))
                        .build();
            }
            case OLLAMA -> OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder()
                            .baseUrl(config.baseUrl())
                            .restClientBuilder(restClientBuilder(config.timeout()))
                            .build())
                    .defaultOptions(OllamaChatOptions.builder()
                            .model(config.model())
                            .temperature(temperature)
                            .build())
                    .retryTemplate(retryTemplate(config.maxAttempts()))
                    .build();
        };
    }

    private static RestClient.Builder restClientBuilder(Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * Remplace le RetryTemplate par défaut de Spring AI (10 tentatives, backoff jusqu'à 3 min),
     * qui bloquerait la bascule vers le fournisseur suivant. Seules les erreurs transitoires
     * (5xx, réseau) sont relancées ; les 4xx — dont le 429 « quota dépassé » — ne le sont pas.
     */
    private static RetryTemplate retryTemplate(int maxAttempts) {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .retryOn(TransientAiException.class)
                .retryOn(ResourceAccessException.class)
                .fixedBackoff(Duration.ofSeconds(1))
                .build();
    }
}
