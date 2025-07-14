package com.interviewradar.config;

import com.interviewradar.llm.MetadataChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DeepseekChatConfig {

    @Bean
    @Qualifier("deepseekChatModel")
    public ChatModel deepseekChatModel(
            @Value("${langchain4j.deepseek.chat.api-key}") String apiKey,
            @Value("${langchain4j.deepseek.chat.base-url}") String baseUrl,
            @Value("${langchain4j.deepseek.chat.model-name}") String modelName,
            @Value("${langchain4j.deepseek.chat.timeout}") Duration timeout) {
        ChatModel delegate = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(timeout)
                .build();

        return new MetadataChatModel() {
            @Override
            public String chat(String prompt) {
                return delegate.chat(prompt);
            }

            @Override
            public String getModelName() {
                return "deepseek";
            }

            @Override
            public String getModelVersion() {
                return modelName;
            }
        };
    }

    @Bean
    @Qualifier("deepseekReasonerModel")
    public ChatModel deepseekReasonerModel(
            @Value("${langchain4j.deepseek.reasoner.api-key}") String apiKey,
            @Value("${langchain4j.deepseek.reasoner.base-url}") String baseUrl,
            @Value("${langchain4j.deepseek.reasoner.model-name}") String modelName,
            @Value("${langchain4j.deepseek.reasoner.timeout}") Duration timeout) {
        ChatModel delegate = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(timeout)
                .build();

        return new MetadataChatModel() {
            @Override
            public String chat(String prompt) {
                return delegate.chat(prompt);
            }

            @Override
            public String getModelName() {
                return "deepseek";
            }

            @Override
            public String getModelVersion() {
                return modelName;
            }
        };
    }
}
