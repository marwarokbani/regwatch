package com.regwatch.llm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    FallbackLlmClient llmClient(LlmProperties properties) {
        return new FallbackLlmClient(new LlmProviderFactory().buildChain(properties));
    }
}
