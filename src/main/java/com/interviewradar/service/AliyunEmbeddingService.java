// src/main/java/com/interviewradar/service/AliyunEmbeddingService.java
package com.interviewradar.service;

import com.openai.client.OpenAIClient;
import com.openai.core.ClientOptions;
import com.openai.core.RequestOptions;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.services.blocking.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Embedding service: provides raw vector output and implements LangChain4j's EmbeddingModel.
 */
@Service
public class AliyunEmbeddingService implements EmbeddingService, EmbeddingModel {

    private final OpenAIClient client;
    private final String model;
    private final int dimension;

    public AliyunEmbeddingService(
            OpenAIClient client,
            @Value("${aliyun.embedding.model}") String model,
            @Value("${aliyun.embedding.dimension}") int dimension
    ) {
        this.client = client;
        this.model = model;
        this.dimension = dimension;
    }

    /**
     * Raw embedding: returns float vector for input text
     */
    public float[] embedRaw(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(model)
                .input(text)
                .dimensions(dimension)
                .build();

        CreateEmbeddingResponse resp = client.embeddings().create(params);
        List<Float> list = resp.data().get(0).embedding();
        float[] vector = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            vector[i] = list.get(i);
        }
        return vector;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // EmbeddingModel implementation
    // ──────────────────────────────────────────────────────────────────────────────

    @Override
    public Response<Embedding> embed(String text) {
        float[] vector = embedRaw(text);
        return Response.from(Embedding.from(vector));
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        List<Embedding> embeddings = segments.stream()
                .map(TextSegment::text)
                .map(this::embedRaw)
                .map(Embedding::from)
                .collect(Collectors.toList());
        return Response.from(embeddings);
    }

    @Override
    public int dimension() {
        return dimension;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // OpenAI EmbeddingService methods
    // ──────────────────────────────────────────────────────────────────────────────

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
