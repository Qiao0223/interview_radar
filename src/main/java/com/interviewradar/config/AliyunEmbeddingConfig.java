package com.interviewradar.config;

import com.interviewradar.service.AliyunEmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.embedding")
public class AliyunEmbeddingConfig {

    /**
     * Bound from application.yml → aliyun.embedding.*
     */
    private String apiKey;
    private String baseUrl;
    private String model;
    private int dimension;

    /**
     * This is the one-and-only EmbeddingModel bean.
     * Mark as @Primary so it wins if the starter auto-configs another.
     */
    @Bean
    @Primary
    public EmbeddingModel aliyunEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Wrap it in your service with the configured dimension.
     */
    @Bean
    public AliyunEmbeddingService aliyunEmbeddingService(EmbeddingModel aliyunEmbeddingModel) {
        return new AliyunEmbeddingService(aliyunEmbeddingModel, dimension);
    }
}
