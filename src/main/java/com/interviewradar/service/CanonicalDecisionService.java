package com.interviewradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.interviewradar.common.Utils;
import com.interviewradar.model.dto.CanonicalQuestionCandidateDTO;
import com.interviewradar.model.entity.CanonicalQuestionEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.ReviewStatus;
import com.interviewradar.model.repository.CanonicalQuestionRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.highlevel.dml.InsertRowsParam;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to decide whether to reuse, skip, or create a new canonical question.
 */
@Service
@RequiredArgsConstructor
public class CanonicalDecisionService {

    private final QuestionRetrievalService retrievalService;
    private final ChatModel chatModel;
    private final CanonicalQuestionRepository canonicalRepo;
    private final ExtractedQuestionRepository extractedRepo;
    private final MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void decide(Long extractedQuestionId) throws Exception {
        ExtractedQuestionEntity extracted = extractedRepo.findById(extractedQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + extractedQuestionId));

        // 1. Retrieve top-K candidates
        List<CanonicalQuestionCandidateDTO> cands = retrievalService.retrieveCandidates(extractedQuestionId);

        // 2. Build decision prompt
        String prompt = buildDecisionPrompt(extracted.getQuestionText(), cands);
        String llmRaw = chatModel.chat(prompt);
        String json = Utils.extractJson(llmRaw);

        // 3. Parse decision
        DecisionResult result = objectMapper.readValue(json, DecisionResult.class);
        switch (result.getAction()) {
            case "REUSE":
                reuse(extracted, result.getChosenId());
                break;
            case "CREATE":
                createNew(extracted, result.getNewText());
                break;
            case "SKIP":
            default:
                // do nothing, leave canonicalized = false
                return;
        }
    }

    private void reuse(ExtractedQuestionEntity extracted, Long canonicalId) {
        CanonicalQuestionEntity cq = canonicalRepo.findById(canonicalId)
                .orElseThrow(() -> new IllegalArgumentException("Canonical not found: " + canonicalId));
        cq.setCount(cq.getCount() + 1);
        cq.setUpdatedAt(LocalDateTime.now());
        canonicalRepo.save(cq);

        extracted.getCanonicalQuestions().add(cq);
        extracted.setCanonicalized(true);
        extracted.setUpdatedAt(LocalDateTime.now());
        extractedRepo.save(extracted);
    }

    private void createNew(ExtractedQuestionEntity extracted, String newText) {
        // persist new canonical question
        CanonicalQuestionEntity cq = CanonicalQuestionEntity.builder()
                .text(newText)
                .category(extracted.getCategories().iterator().next())
                .status(ReviewStatus.PENDING)
                .count(1)
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        cq = canonicalRepo.save(cq);

// 1. 计算向量
        float[] vector = embeddingModel.embed(newText).content().vector();

// 2. 构造 JsonObject 表示一行
        JsonObject row = new JsonObject();
        row.addProperty("id", cq.getId());
        row.addProperty("text", newText);

// 把 float[] 转成 JsonArray
        JsonArray arr = new JsonArray();
        for (float v : vector) {
            arr.add(v);
        }
        row.add("question_embedding", arr);

        row.addProperty("category_id", cq.getCategory().getId());
        row.addProperty("status", cq.getStatus().ordinal());

// 3. 包装成 List<JsonObject>
        List<JsonObject> rows = Collections.singletonList(row);

// 4. 构建 InsertRowsParam 并执行
        InsertRowsParam insertRowsParam = InsertRowsParam.newBuilder()
                .withCollectionName("canonical_question")
                .withRows(rows)
                .build();

// 5. 取出底层 InsertParam 并插入
        milvusClient.insert(insertRowsParam.getInsertParam());

        // map and save
        extracted.getCanonicalQuestions().add(cq);
        extracted.setCanonicalized(true);
        extracted.setUpdatedAt(LocalDateTime.now());
        extractedRepo.save(extracted);
    }

    private String buildDecisionPrompt(String questionText, List<CanonicalQuestionCandidateDTO> cands) {
        StringBuilder sb = new StringBuilder();
        sb.append("给定原始问题：\n");
        sb.append(questionText).append("\n");
        sb.append("以及以下标准化候选：\n");
        for (int i = 0; i < cands.size(); i++) {
            var c = cands.get(i);
            sb.append(i + 1).append(". [").append(c.getStatus())
                    .append("] ").append(c.getText()).append(" (id=").append(c.getId())
                    .append(")，Score=").append(c.getScore()).append("\n");
        }
        sb.append("请返回 JSON 对象，字段 action 可为 REUSE, SKIP, CREATE；")
                .append("若为 REUSE，请给出 chosenId；若为 CREATE，请给出 newText。\n示例：{\"action\":\"REUSE\",\"chosenId\":123}" );
        return sb.toString();
    }

    // helper DTO for parsing LLM decision
    @Data
    private static class DecisionResult {
        private String action;
        private Long chosenId;
        private String newText;

    }
}
