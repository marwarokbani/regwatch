package com.regwatch.llm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LlmTestController.class)
@ActiveProfiles("dev")
class LlmTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FallbackLlmClient llmClient;

    @Test
    void returnsProviderUsedForTheCall() throws Exception {
        when(llmClient.chat(any())).thenReturn(new LlmResponse("Bonjour", "groq", "llama", 120, 30, 5));

        mockMvc.perform(post("/api/llm/test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Dis bonjour\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Bonjour"))
                .andExpect(jsonPath("$.provider").value("groq"));
    }

    @Test
    void rejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/llm/test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns503ProblemDetailWhenWholeChainFails() throws Exception {
        when(llmClient.chat(any())).thenThrow(new LlmUnavailableException(List.of("groq → 429")));

        mockMvc.perform(post("/api/llm/test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Dis bonjour\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("LLM indisponible"))
                .andExpect(jsonPath("$.failures[0]").value("groq → 429"));
    }
}
