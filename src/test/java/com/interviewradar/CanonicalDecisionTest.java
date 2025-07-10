package com.interviewradar;

import com.interviewradar.model.entity.CanonicalQuestionEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.entity.CategoryEntity;
import com.interviewradar.model.repository.CanonicalQuestionRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.service.CanonicalDecisionService;
import com.interviewradar.service.QuestionRetrievalService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.highlevel.dml.InsertRowsParam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CanonicalDecisionTest {

    @Autowired
    private QuestionRetrievalService retrievalService;

    @Autowired
    private CanonicalDecisionService decisionService;

    @Autowired
    private ExtractedQuestionRepository extractedRepo;

    @Autowired
    private CanonicalQuestionRepository canonicalRepo;

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void fullFlow_reuseScenario() throws Exception {
        // 1. 准备一条原始抽取问题
        CategoryEntity cat = CategoryEntity.builder()
                .id(1L)
                .name("TestCat")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ExtractedQuestionEntity extracted = ExtractedQuestionEntity.builder()
                .questionText("原始问题？")
                .canonicalized(false)
                .categorized(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        extracted.setCategories(Collections.singleton(cat));
        extracted = extractedRepo.save(extracted);

        // 2. 准备一条候选标准问法并 Upsert 到 Milvus
        CanonicalQuestionEntity canonical = CanonicalQuestionEntity.builder()
                .text("标准化问法")
                .status(com.interviewradar.model.ReviewStatus.APPROVED)
                .count(1)
                .createdBy("init")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .category(cat)
                .build();
        canonical = canonicalRepo.save(canonical);

        float[] vector = embeddingModel.embed(canonical.getText()).content().vector();
        com.google.gson.JsonObject row = new com.google.gson.JsonObject();
        row.addProperty("id", canonical.getId());
        row.addProperty("text", canonical.getText());
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (float v : vector) arr.add(v);
        row.add("question_embedding", arr);
        row.addProperty("category_id", cat.getId());
        row.addProperty("status", canonical.getStatus().ordinal());
        List<com.google.gson.JsonObject> rows = Collections.singletonList(row);
        InsertRowsParam param = InsertRowsParam.newBuilder()
                .withCollectionName("canonical_question")
                .withRows(rows)
                .build();
        milvusClient.insert(param.getInsertParam());

        // 3. 执行决策
        decisionService.decide(extracted.getId());

        // 4. 断言：extracted 已关联并标记
        ExtractedQuestionEntity updated = extractedRepo.findById(extracted.getId()).orElseThrow();
        assertThat(updated.getCanonicalized()).isTrue();
        assertThat(updated.getCanonicalQuestions())
                .extracting(CanonicalQuestionEntity::getId)
                .contains(canonical.getId());
    }

//    @TestConfiguration
//    static class ChatModelStubConfig {
//        @Bean
//        @Primary
//        public dev.langchain4j.model.chat.ChatModel chatModelStub() {
//            return prompt -> {
//                // 简单判断 Prompt 内容，返回复用决策 JSON
//                if (prompt.contains("给定原始问题")) {
//                    // 从 JSON 中提取第一候选 id
//                    String idStr = prompt.replaceAll("(?s).*\(id=(\\d+)\\).*", "$1");
//                    return String.format("{\"action\":\"REUSE\",\"chosenId\":%s}", idStr);
//                }
//                return "{\"action\":\"SKIP\"}";
//            };
//        }
//    }
}
