// src/main/java/com/interviewradar/service/AliyunEmbeddingService.java
package com.interviewradar.service;

import com.openai.client.OpenAIClient;
import com.openai.core.ClientOptions;
import com.openai.core.RequestOptions;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.services.blocking.EmbeddingService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class AliyunEmbeddingService implements EmbeddingService {
    private final OpenAIClient client;
    private final String model;
    private final Integer dimension;

    public AliyunEmbeddingService(OpenAIClient client,
                                  @Value("${aliyun.embedding.model}") String model,
                                  @Value("${aliyun.embedding.dimension}") Integer dimension) {
        this.client = client;
        this.model = model;
        this.dimension = dimension;
    }

    public float[] embed(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(model)
                .input(text)
                .dimensions(dimension)
                .build();

        CreateEmbeddingResponse resp = client.embeddings().create(params);

        List<Float> embedding = resp.data().get(0).embedding();
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i);
        }
        return result;
    }

    @NotNull
    @Override
    public WithRawResponse withRawResponse() {
        return client.embeddings().withRawResponse();
    }

    @NotNull
    @Override
    public EmbeddingService withOptions(@NotNull Consumer<ClientOptions.Builder> consumer) {
        return client.embeddings().withOptions(consumer);
    }

    @NotNull
    @Override
    public CreateEmbeddingResponse create(@NotNull EmbeddingCreateParams embeddingCreateParams,
                                          @NotNull RequestOptions requestOptions) {
        return client.embeddings().create(embeddingCreateParams, requestOptions);
    }
}