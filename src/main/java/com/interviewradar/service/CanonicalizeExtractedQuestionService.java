package com.interviewradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.common.Utils;
import com.interviewradar.config.ClassificationProperties;
import com.interviewradar.llm.PromptTemplate;
import com.interviewradar.model.entity.CandidateCanonicalQuestionEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.enums.CandidateStatus;
import com.interviewradar.model.repository.CandidateCanonicalQuestionRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务：将提取的问题批量标准化为候选标准问题
 * 使用多字段 JSON 格式保持一对多对应关系，并显示原问题与标准化结果映射
 */
@Service
@RequiredArgsConstructor
public class CanonicalizeExtractedQuestionService {

    @Lazy
    @Autowired
    private CanonicalizeExtractedQuestionService selfProxy;

    private final ExtractedQuestionRepository extractedQuestionRepo;
    private final CandidateCanonicalQuestionRepository candidateRepo;
    private final ChatModel chatModel;
    private final AliyunEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final ClassificationProperties props;

    /**
     * 分批标准化提取的问题列表，每批独立事务
     */
    public void batchStandardize(List<ExtractedQuestionEntity> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        int batchSize = props.getBatchSize();
        for (int i = 0; i < questions.size(); i += batchSize) {
            List<ExtractedQuestionEntity> batch = questions.subList(i, Math.min(i + batchSize, questions.size()));
            try {
                selfProxy.processBatch(batch);
            } catch (Exception e) {
                System.err.println("标准化批次失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理一批问题，独立事务，避免回滚影响其他批次
     * 并在控制台显示原问题与对应的标准化结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<ExtractedQuestionEntity> batch) {
        String prompt = buildBatchPrompt(batch);
        String raw = chatModel.chat(prompt);
        List<StandardItem> items = parseStandardizedItems(raw);

        for (StandardItem item : items) {
            int idx = item.getIndex() - 1;
            if (idx < 0 || idx >= batch.size()) continue;

            ExtractedQuestionEntity src = batch.get(idx);
            // 过滤空标题
            List<String> validTitles = item.getTitles().stream()
                    .filter(title -> !StringUtils.isBlank(title))
                    .collect(Collectors.toList());
            if (!validTitles.isEmpty()) {
                // 显示原问题及对应标准化候选
                System.out.printf("[STANDARDIZATION] 原问题: %s | 标准化候选: %s%n",
                        src.getQuestionText(),
                        String.join(" | ", validTitles));
            }
            boolean added = false;
            // 保存候选记录
            for (String title : validTitles) {
                float[] vector = embeddingService.embedRaw(title);
                String embeddingJson = Utils.toJsonArray(vector);

                CandidateCanonicalQuestionEntity candidate = CandidateCanonicalQuestionEntity.builder()
                        .text(title)
                        .embedding(embeddingJson)
                        .sourceQuestion(src)
                        .status(CandidateStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                candidateRepo.save(candidate);
                added = true;
            }

            if (added) {
                src.setCanonicalized(true); // 标记为已标准化
                src.setUpdatedAt(LocalDateTime.now()); // 更新更新时间
            }
        }

        // 批量更新 canonicalized 字段并保存变更
        extractedQuestionRepo.saveAll(batch);
    }

    /**
     * 构建批量标准化 Prompt
     */
    private String buildBatchPrompt(List<ExtractedQuestionEntity> batch) {
        String template = PromptTemplate.QUESTION_STANDARDIZATION.getTemplate();
        String questions = batch.stream()
                .map(ExtractedQuestionEntity::getQuestionText)
                .collect(Collectors.joining("\n"));
        return template
                .replace("${n}", String.valueOf(batch.size()))
                .replace("${questions}", questions);
    }

    /**
     * 解析 LLM 返回 JSON 为结构化结果
     */
    private List<StandardItem> parseStandardizedItems(String raw) {
        String json = Utils.extractJsonArray(raw);
        try {
            return objectMapper.readValue(json, new TypeReference<List<StandardItem>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析标准化结果失败: " + json, e);
        }
    }

    @Data
    private static class StandardItem {
        private int index;
        private List<String> titles;
    }
}
