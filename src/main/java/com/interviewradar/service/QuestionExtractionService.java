package com.interviewradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.data.entity.InterviewEntity;
import com.interviewradar.data.entity.QuestionEntity;
import com.interviewradar.data.repository.InterviewRepository;
import com.interviewradar.data.repository.QuestionRepository;
import com.interviewradar.llm.LanguageModel;
import com.interviewradar.llm.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.interviewradar.common.Utils.extractJson;

/**
 * 基于大语言模型（LLM）从原始面经中抽取面试题目的服务类
 */
@Service
public class QuestionExtractionService {

    @Lazy
    @Autowired
    private QuestionExtractionService selfProxy;

    private final LanguageModel llm; // 大语言模型接口
    private final QuestionRepository questionRepo; // 问题存储仓库
    private final InterviewRepository interviewRepo; // 面经存储仓库
    private final ObjectMapper mapper = new ObjectMapper(); // 用于解析 JSON

    public QuestionExtractionService(LanguageModel llm,
                                     QuestionRepository questionRepo,
                                     InterviewRepository interviewRepo) {
        this.llm = llm;
        this.questionRepo = questionRepo;
        this.interviewRepo  = interviewRepo;
    }

    /**
     * 从原始面经内容中提取问题文本列表
     * @param rawInterview 原始面经内容
     * @return 提取出的面试问题列表
     * @throws Exception 如果解析或 LLM 调用失败
     */
    public List<String> extractQuestions(String rawInterview) throws Exception {
        // 1. 构造 prompt，将原始面经注入模板
        String prompt = PromptTemplate.QUESTION_EXTRACTION.getTemplate()
                .replace("{rawInterview}", rawInterview);

        // 2. 调用大语言模型生成响应
        String llmResponse = llm.generate(prompt);

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
            InterviewEntity interview = interviewRepo.getReferenceById(interviewId);
            interview.setQuestionsExtracted(true);
            interviewRepo.save(interview);
            // 提取问题
            List<String> questions = extractQuestions(rawInterview);
            List<QuestionEntity> entities = new ArrayList<>();
            for (String q : questions) {
                // 构建问题实体对象
                QuestionEntity qe = QuestionEntity.builder()
                        .interview(interviewRepo.getReferenceById(interviewId))
                        .questionText(q)
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
    public void extractAndSave(InterviewEntity interview) {
        selfProxy.extractAndSave(interview.getContentId(), interview.getContent());
    }

    /**
     * 批量提取：输入多个 InterviewEntity，逐条调用 LLM 提取并保存
     * @param interviews 面经实体列表
     */
    public void extractAndSave(List<InterviewEntity> interviews) {
        for (InterviewEntity interview : interviews) {
            // 逐条提取，确保每次调用都走事务代理
            selfProxy.extractAndSave(interview);
        }
    }
}
