package com.interviewradar.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 把 LangChain4j 的 EmbeddingModel delegate 和 dimension 包装在一起，
 * 暴露原始向量 embedRaw 以及标准的 embed/embedAll 方法。
 */
@Service
public class AliyunEmbeddingService implements EmbeddingModel {

    private final EmbeddingModel delegate;
    public AliyunEmbeddingService(EmbeddingModel delegate) {
        this.delegate = delegate;
    }

    /**
     * 拿到最原始的 float[] 向量
     */
    public float[] embedRaw(String text) {
        Response<Embedding> resp = delegate.embed(text);
        return resp.content().vector();
    }

    @Override
    public Response<Embedding> embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return delegate.embed(textSegment);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        return delegate.embedAll(segments);
    }
}
