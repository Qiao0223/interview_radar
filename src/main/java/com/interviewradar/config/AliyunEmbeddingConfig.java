package com.interviewradar.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.services.blocking.EmbeddingService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.interviewradar.service.AliyunEmbeddingService;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.embedding")
public class AliyunEmbeddingConfig {

    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer dimension;

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public EmbeddingService embeddingService(OpenAIClient client) {
        return new AliyunEmbeddingService(client, model, dimension);
    }
}
