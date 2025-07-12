package com.interviewradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.model.repository.RawInterviewRepository;
import com.interviewradar.llm.PromptTemplate;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.interviewradar.common.Utils.extractJson;

/**
 * 基于大语言模型（LLM）从原始面经中抽取面试题目的服务类
 */
@RequiredArgsConstructor
@Service
public class RawQuestionExtractionService {

    @Lazy
    @Autowired
    private RawQuestionExtractionService selfProxy;

    private final ChatModel chatModel; // LangChain4j Chat model
    private final RawQuestionRepository questionRepo; // 问题存储仓库
    private final RawInterviewRepository interviewRepo; // 面经存储仓库
    private final ObjectMapper mapper = new ObjectMapper(); // 用于解析 JSON

    /**
     * 从原始面经内容中提取问题文本列表
     * @param rawInterview 原始面经内容
     * @return 提取出的面试问题列表
     * @throws Exception 如果解析或 LLM 调用失败
     */
    public List<String> extractQuestions(String rawInterview) throws Exception {
        // 1. 构造 prompt，将原始面经注入模板
        dev.langchain4j.model.input.PromptTemplate template =
                dev.langchain4j.model.input.PromptTemplate.from(
                        PromptTemplate.QUESTION_EXTRACTION.getTemplate());
        String prompt = template.apply(java.util.Map.of("rawInterview", rawInterview)).text();
        //System.out.println("最终构造的 prompt >>>\n" + prompt);
        // 2. 调用大语言模型生成响应
        String llmResponse = chatModel.chat(prompt);

        System.out.println("原始面经 >>>\n" + rawInterview);
        System.out.println("LLM 原始返回 >>>\n" + llmResponse);

        // 3. 解析 JSON 响应：格式应为 { "questions": ["问题1", "问题2", ... ] }
        String jsonOnly = extractJson(llmResponse);
        JsonNode root = mapper.readTree(jsonOnly);
        JsonNode arr = root.path("questions");
        if (!arr.isArray()) {
            throw new IllegalStateException("LLM 输出格式非法: " + llmResponse);
        }

        // 4. 收集非空问题
        List<String> questions = new ArrayList<>();
        for (JsonNode node : arr) {
            String text = node.asText("").trim();
            if (!text.isEmpty()) {
                questions.add(text);
            }
        }
        return questions;
    }

    /**
     * 为指定的面经提取问题并保存到数据库
     * @param interviewId 面经 ID
     * @param rawInterview 原始面经内容
     */
    @Transactional
    public void extractAndSave(Long interviewId, String rawInterview) {
        try {
            // 标记为已提取
            RawInterview interview = interviewRepo.getReferenceById(interviewId);
            interview.setQuestionsExtracted(true);
            interviewRepo.save(interview);
            // 提取问题
            List<String> questions = extractQuestions(rawInterview);
            List<RawQuestion> entities = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (String q : questions) {
                RawQuestion qe = RawQuestion.builder()
                        .interview(interview)
                        .questionText(q)
                        .candidatesGenerated(false)
                        .categoriesAssigned(false)
                        .createdAt( Instant.now())
                        .updatedAt( Instant.now())
                        .build();
                entities.add(qe);
            }
            // 批量保存
            questionRepo.saveAll(entities);
        } catch (Exception e) {
            // TODO: 可以添加重试逻辑或记录日志
            throw new RuntimeException("面经提问提取与保存失败 interviewId=" + interviewId, e);
        }
    }

    /**
     * 重载方法：从 InterviewEntity 中提取并保存问题
     */
    public void extractAndSave(RawInterview interview) {
        selfProxy.extractAndSave(interview.getId(), interview.getContent());
    }
}
