package com.interviewradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.llm.PromptContext;
import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.enums.TaskType;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.model.repository.RawInterviewRepository;
import com.interviewradar.llm.PromptTemplate;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.interviewradar.common.Utils.extractJson;

/**
 * 基于大语言模型（LLM）从原始面经中抽取面试题目的服务类
 */
@Service
public class RawQuestionExtractionService {

    @Lazy
    @Autowired
    private RawQuestionExtractionService selfProxy;

    private static final Logger log = LoggerFactory.getLogger(RawQuestionExtractionService.class);

    private final ChatModel chatModel;
    private final RawQuestionRepository questionRepo;
    private final RawInterviewRepository interviewRepo;
    private final ObjectMapper mapper;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    public RawQuestionExtractionService(
            @Qualifier("deepseekReasonerModel")
            ChatModel chatModel,
            RawQuestionRepository questionRepo,
            RawInterviewRepository interviewRepo,
            ObjectMapper mapper, ThreadPoolTaskExecutor taskExecutor
            ) {
        this.chatModel = chatModel;
        this.questionRepo = questionRepo;
        this.interviewRepo = interviewRepo;
        this.mapper = mapper;
        this.taskExecutor = taskExecutor;
    }


    /**
     * 从原始面经内容中提取问题文本列表
     * @param rawInterview 原始面经内容
     * @return 提取出的面试问题列表
     * @throws Exception 如果解析或 LLM 调用失败
     */
    public List<String> extractQuestions(Long interviewId, String rawInterview) throws Exception {
        // 1. 构造 prompt，将原始面经注入模板
        dev.langchain4j.model.input.PromptTemplate template =
                dev.langchain4j.model.input.PromptTemplate.from(
                        PromptTemplate.QUESTION_EXTRACTION.getTemplate());
        String prompt = template.apply(Map.of("rawInterview", rawInterview)).text();
        //System.out.println("最终构造的 prompt >>>\n" + prompt);
        // 2. 调用大语言模型生成响应
        PromptContext.add(TaskType.EXTRACTION, interviewId);
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
            List<String> questions = extractQuestions(interviewId, rawInterview);
            List<RawQuestion> entities = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (String q : questions) {
                RawQuestion qe = RawQuestion.builder()
                        .interview(interview)
                        .questionText(q)
                        .candidatesGenerated(false)
                        .categoriesAssigned(false)
                        .createdAt( LocalDateTime.now())
                        .updatedAt( LocalDateTime.now())
                        .build();
                entities.add(qe);
            }
            // 批量保存
            questionRepo.saveAll(entities);
        } catch (Exception e) {
            throw new RuntimeException("面经提问提取与保存失败 interviewId=" + interviewId, e);
        }
    }

    /**
     * 重载方法：从 InterviewEntity 中提取并保存问题
     */
    public void extractAndSave(RawInterview interview) {
        selfProxy.extractAndSave(interview.getId(), interview.getContent());
    }

    /**
     * 无参方法：批量拉取所有未提取的面经并执行提取保存
     */
    public void extractAllInterviews() {
        List<RawInterview> pending = interviewRepo.findByQuestionsExtractedFalse();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (RawInterview interview : pending) {
            futures.add(
                    CompletableFuture.runAsync(() -> {
                        try {
                            selfProxy.extractAndSave(interview);
                        } catch (Exception e) {
                            System.err.println("面经提取失败 interviewId=" + interview.getId() + ": " + e.getMessage());
                        }
                    }, taskExecutor)
            );
        }

        // 等待所有并发任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
