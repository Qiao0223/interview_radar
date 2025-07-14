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
public class AliyunChatConfig {

    @Bean
    @Qualifier("aliyunChatModel")
    public ChatModel aliyunChatModel(
            @Value("${langchain4j.aliyun.qwen-plus.api-key}") String apiKey,
            @Value("${langchain4j.aliyun.qwen-plus.base-url}") String baseUrl,
            @Value("${langchain4j.aliyun.qwen-plus.model-name}") String modelName,
            @Value("${langchain4j.aliyun.qwen-plus.timeout}") Duration timeout) {

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
                return "aliyun";
            }

            @Override
            public String getModelVersion() {
                return modelName;
            }
        };
    }
}

