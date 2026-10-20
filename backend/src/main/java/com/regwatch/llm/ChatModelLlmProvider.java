package com.regwatch.llm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;

/**
 * Adapte n'importe quel {@link ChatModel} Spring AI à notre port.
 * <p>
 * Une seule classe suffit pour tous les fournisseurs : les différences (URL, clé, format d'API)
 * sont déjà absorbées par les implémentations Spring AI construites dans {@link LlmProviderFactory}.
 */
public final class ChatModelLlmProvider implements LlmProvider {

    private final String name;
    private final String model;
    private final ChatModel chatModel;

    public ChatModelLlmProvider(String name, String model, ChatModel chatModel) {
        this.name = name;
        this.model = model;
        this.chatModel = chatModel;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.systemPrompt())) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        messages.add(new UserMessage(request.userPrompt()));

        // Options génériques : Spring AI les fusionne avec les options par défaut du modèle
        // (nom du modèle, température configurée), les champs null n'écrasent rien.
        Prompt prompt = request.temperature() == null
                ? new Prompt(messages)
                : new Prompt(messages, ChatOptions.builder().temperature(request.temperature()).build());

        long start = System.nanoTime();
        ChatResponse response = chatModel.call(prompt);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        String content = response == null || response.getResult() == null
                ? null
                : response.getResult().getOutput().getText();
        if (!StringUtils.hasText(content)) {
            // Une réponse vide est traitée comme un échec pour que la chaîne passe au fournisseur suivant.
            throw new IllegalStateException("Réponse vide du fournisseur " + name);
        }

        Usage usage = response.getMetadata().getUsage();
        return new LlmResponse(content, name, model, latencyMs,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens());
    }
}
