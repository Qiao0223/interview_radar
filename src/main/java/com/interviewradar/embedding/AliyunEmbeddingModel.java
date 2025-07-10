package com.interviewradar.embedding;

import com.interviewradar.service.AliyunEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.stream.Collectors;

public class AliyunEmbeddingModel implements EmbeddingModel {

    private final AliyunEmbeddingService aliyunEmbeddingService;

    public AliyunEmbeddingModel(AliyunEmbeddingService aliyunEmbeddingService) {
        this.aliyunEmbeddingService = aliyunEmbeddingService;
    }

    @Override
    public Response<Embedding> embed(String text) {
        float[] vector = aliyunEmbeddingService.embed(text);
        Embedding embedding = new Embedding(vector);
        return Response.from(embedding);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        List<Embedding> embeddings = segments.stream()
                .map(TextSegment::text)
                .map(this::embed)
                .map(Response::content)
                .collect(Collectors.toList());
        return Response.from(embeddings);
    }
}
