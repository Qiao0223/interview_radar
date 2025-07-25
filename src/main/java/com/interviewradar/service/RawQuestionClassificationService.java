package com.interviewradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.common.Utils;
import com.interviewradar.config.ClassificationProperties;
import com.interviewradar.llm.PromptContext;
import com.interviewradar.model.entity.Category;
import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.entity.RawQuestionCategory;
import com.interviewradar.model.entity.RawQuestionCategoryId;
import com.interviewradar.model.enums.TaskType;
import com.interviewradar.model.repository.CategoryRepository;
import com.interviewradar.model.repository.RawQuestionCategoryRepository;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.llm.PromptTemplate;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 分类服务：调用 LLM 将题目按类别进行批量分类
 */
@Service
public class RawQuestionClassificationService {
    @Lazy
    @Autowired
    RawQuestionClassificationService selfProxy;

    private static final Logger log = LoggerFactory.getLogger(RawQuestionClassificationService.class);

    private final ChatModel chatModel;                  // LangChain4j Chat model
    private final CategoryRepository categoryRepo;    // 类别仓库，读取所有可用类别
    private final RawQuestionRepository questionRepo;    // 题目仓库，用于保存分类结果
    private final ClassificationProperties props;     // batchSize 配置
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 对象映射器，用于解析 LLM 返回的 JSON
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RawQuestionCategoryRepository rawQuestionCategoryRepo;

    public RawQuestionClassificationService(
            @Qualifier("deepseekChatModel")ChatModel chatModel,
            CategoryRepository categoryRepo,
            RawQuestionRepository questionRepo,
            ClassificationProperties props,
            ThreadPoolTaskExecutor taskExecutor, RawQuestionCategoryRepository rawQuestionCategoryRepo) {
        this.chatModel = chatModel;
        this.categoryRepo = categoryRepo;
        this.questionRepo = questionRepo;
        this.props = props;
        this.taskExecutor = taskExecutor;
        this.rawQuestionCategoryRepo = rawQuestionCategoryRepo;
    }


    public void classifyBatch(List<RawQuestion> questions) {
        int batchSize = props.getBatchSize();
        List<Category> allCats = categoryRepo.findAll();
        String formattedCats = formatCategories(allCats);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < questions.size(); i += batchSize) {
            List<RawQuestion> batch = questions.subList(i, Math.min(i + batchSize, questions.size()));

            futures.add(
                    CompletableFuture.runAsync(
                            () -> selfProxy.processBatch(batch, formattedCats),
                            taskExecutor
                    )
            );
        }

        // 等待所有并发任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 处理一批题目，打印原问题和分类名称
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<RawQuestion> batch, String formattedCats) {

        // 构造上下文
        List<Long> idList = batch.stream()
                .map(RawQuestion::getId)
                .toList();
        PromptContext.addBatch(TaskType.CLASSIFICATION, idList);

        String prompt = buildBatchPrompt(batch, formattedCats);
        String raw = chatModel.chat(prompt);
        String json = Utils.extractJsonArray(raw);
        Map<Integer, List<Long>> map = parseBatchResult(json);

        for (int idx = 0; idx < batch.size(); idx++) {
            RawQuestion detached = batch.get(idx);
            List<Long> chosen = map.get(idx + 1);
            String questionText = detached.getQuestionText();

            if (chosen == null || chosen.isEmpty()) {
                System.out.println("原问题: " + questionText + " -> 未分到任何类别");
                continue;
            }

            RawQuestion q = questionRepo.findById(detached.getId())
                    .orElseThrow(() -> new IllegalStateException("找不到问题实体 id=" + detached.getId()));

            List<String> categoryNames = new ArrayList<>();

            for (Long catId : chosen) {
                Category c = categoryRepo.findById(catId)
                        .orElseThrow(() -> new IllegalStateException("找不到分类 id=" + catId));

                RawQuestionCategory link = RawQuestionCategory.builder()
                        .id(new RawQuestionCategoryId(q.getId(), c.getId()))
                        .rawQuestion(q)
                        .category(c)
                        .assignedAt(LocalDateTime.now())
                        .build();

                rawQuestionCategoryRepo.save(link);
                categoryNames.add(c.getName());
            }
            q.setCategoriesAssigned(true);
            questionRepo.save(q);

            // 打印原问题及分类结果
            log.info("Thread ID:{},原问题:{}->分类:{}", Thread.currentThread().getId(), questionText, categoryNames);
        }
    }
    /**
     * 构建每批题目的 LLM Prompt
     * @param batch 当前题目子列表
     * @param formattedCats 已格式化的所有类别列表字符串
     * @return 替换完占位符后的完整 Prompt 文本
     */
    private String buildBatchPrompt(List<RawQuestion> batch, String formattedCats) {
        // 获取原始模板
        String template = PromptTemplate.QUESTION_CLASSIFICATION.getTemplate();
        // 替换批次数量与分类列表
        template = template.replace("${n}", String.valueOf(batch.size()))
                .replace("${categories}", formattedCats);
        // 截取到“问题列表：”这一行，后续自定义题目列表
        String header = "面试题列表：";
        int pos = template.indexOf(header);
        String prefix = template.substring(0, pos + header.length());
        // 构建题目清单，每行为“序号. 问题文本”
        String questionList = batch.stream()
                .map(q -> (batch.indexOf(q) + 1) + ". " + q.getQuestionText())
                .collect(Collectors.joining("\n"));
        return prefix + "\n" + questionList;
    }


    /**
     * 解析 LLM 返回的批量分类 JSON 结果
     * @param json 提取后的 JSON 字符串
     * @return Map：键为题目序号（1-based），值为该题选中的类别 ID 列表
     */
    private Map<Integer, List<Long>> parseBatchResult(String json) {
        try {
            // 将 JSON 反序列化为 BatchItem 列表
            List<BatchItem> items = objectMapper.readValue(json, new TypeReference<List<BatchItem>>() {});
            Map<Integer, List<Long>> map = new HashMap<>();
            // 将每个条目按 index 与类别列表映射到 Map 中
            for (BatchItem it : items) {
                map.put(it.getIndex(), it.getCategories());
            }
            return map;
        } catch (Exception e) {
            // 解析失败时抛出运行时异常，并附带原始 JSON 以便排查
            throw new RuntimeException("解析批量分类结果失败: " + json, e);
        }
    }

    /**
     * 将所有类别格式化为“ID. 名称（描述）”的多行文本，供 LLM 消费
     */
    private String formatCategories(List<Category> categories) {
        return categories.stream()
                .map(c -> c.getId() + ". " + c.getName() + "（" + c.getDescription() + "）")
                .collect(Collectors.joining("\n"));
    }

    public void classifyAllRawQuestions() {
        List<RawQuestion> allRawQuestions = questionRepo.findByCategoriesAssignedFalse();
        try{
            selfProxy.classifyBatch(allRawQuestions);
        }catch (Exception e){
            System.err.println("分类失败");
        }
    }

    /**
     * 内部使用的批量分类结果条目类
     */
    @Data
    private static class BatchItem {
        private int index;             // 题目在本批次中的序号
        private List<Long> categories; // LLM 返回的类别 ID 列表

    }
}
