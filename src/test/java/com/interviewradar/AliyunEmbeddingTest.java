package com.interviewradar;

import com.interviewradar.service.AliyunEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AliyunEmbeddingTest {

    @Autowired
    private AliyunEmbeddingService embeddingService;

    @Test
    void testEmbedReturnsValidVector() {
        String text = "Hello, Langchain!";

        // 改为调用 embedRaw()，这是你保留的原始 float[] 方法
        float[] vector = embeddingService.embedRaw(text);

        assertNotNull(vector, "Embedding vector should not be null");
        assertEquals(1024, vector.length, "Embedding dimension should match configuration");

        for (float v : vector) {
            assertFalse(Float.isNaN(v), "Embedding values should not be NaN");
            assertFalse(Float.isInfinite(v), "Embedding values should not be Infinite");
        }
    }

    @Test
    void testLangChainEmbeddingInterface() {
        String text = "Hello, Langchain!";
        Response<Embedding> response = embeddingService.embed(text);

        assertNotNull(response, "Response should not be null");

        @NonNull Embedding embedding = response.content();
        assertNotNull(embedding, "Embedding content should not be null");

        assertEquals(1024, embedding.vector().length, "Dimension must match config");
    }
}

