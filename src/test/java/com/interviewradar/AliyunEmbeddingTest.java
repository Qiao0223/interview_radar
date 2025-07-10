package com.interviewradar;

import com.interviewradar.service.AliyunEmbeddingService;
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
        float[] vector = embeddingService.embed(text);
        assertNotNull(vector, "Embedding vector should not be null");
        assertEquals(1024, vector.length, "Embedding dimension should match configuration");
        for (float v : vector) {
            assertFalse(Float.isNaN(v), "Embedding values should not be NaN");
            assertFalse(Float.isInfinite(v), "Embedding values should not be Infinite");
        }
    }
}
