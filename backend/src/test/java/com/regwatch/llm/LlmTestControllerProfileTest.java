package com.regwatch.llm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Sans le profil dev, l'endpoint de test n'est pas exposé. */
@WebMvcTest(LlmTestController.class)
class LlmTestControllerProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEndpointIsAbsentOutsideDevProfile() throws Exception {
        mockMvc.perform(post("/api/llm/test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Dis bonjour\"}"))
                .andExpect(status().isNotFound());
    }
}
