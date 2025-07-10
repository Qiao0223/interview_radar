package com.interviewradar.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatBaseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.open-ai.chat-model.timeout}")
    private Duration chatTimeout;

    @Primary
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(chatApiKey)
                .baseUrl(chatBaseUrl)
                .modelName(chatModelName)
                .timeout(chatTimeout)
                .build();
    }
}
