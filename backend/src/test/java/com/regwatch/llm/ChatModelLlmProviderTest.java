package com.regwatch.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class ChatModelLlmProviderTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final ChatModelLlmProvider provider = new ChatModelLlmProvider("groq", "llama", chatModel);

    @Test
    void sendsSystemAndUserMessagesAndNormalizesResponse() {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("Bonjour !"));

        LlmResponse response = provider.chat(new LlmRequest("Tu es un juge.", "Salut", 0.2));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions())
                .extracting(m -> m.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(prompt.getValue().getOptions().getTemperature()).isEqualTo(0.2);
        assertThat(response.content()).isEqualTo("Bonjour !");
        assertThat(response.provider()).isEqualTo("groq");
        assertThat(response.model()).isEqualTo("llama");
    }

    @Test
    void omitsSystemMessageWhenAbsent() {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("ok"));

        provider.chat(LlmRequest.of(null, "Salut"));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(1);
    }

    @Test
    void treatsEmptyResponseAsFailureSoThatChainFallsBack() {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(""));

        assertThatThrownBy(() -> provider.chat(LlmRequest.of(null, "Salut")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
