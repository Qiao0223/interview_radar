package com.interviewradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.common.Utils;
import com.interviewradar.config.ClassificationProperties;
import com.interviewradar.model.entity.CategoryEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.repository.CategoryRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.llm.PromptTemplate;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 分类服务：调用 LLM 将题目按类别进行批量分类
 */
@Service
public class ClassificationService {
    @Lazy
    @Autowired
    ClassificationService selfProxy;

    private final ChatModel chatModel;                  // LangChain4j Chat model
    private final CategoryRepository categoryRepo;    // 类别仓库，读取所有可用类别
    private final ExtractedQuestionRepository questionRepo;    // 题目仓库，用于保存分类结果
    private final ClassificationProperties props;     // batchSize 配置
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 对象映射器，用于解析 LLM 返回的 JSON
    private final ThreadPoolTaskExecutor taskExecutor;

    public ClassificationService(ChatModel chatModel,
                                 CategoryRepository categoryRepo,
                                 ExtractedQuestionRepository questionRepo,
                                 ClassificationProperties props,
                                 @Qualifier("taskExecutor") TaskExecutor taskExecutor) {
        this.chatModel = chatModel;
        this.categoryRepo = categoryRepo;
        this.questionRepo = questionRepo;
        this.props = props;
        this.taskExecutor = (ThreadPoolTaskExecutor) taskExecutor;
    }

    public void classifyBatch(List<ExtractedQuestionEntity> questions) {
        int batchSize = props.getBatchSize();
        List<CategoryEntity> allCats = categoryRepo.findAll();
        String formattedCats = formatCategories(allCats);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < questions.size(); i += batchSize) {
            List<ExtractedQuestionEntity> batch = questions.subList(i, Math.min(i + batchSize, questions.size()));

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
    public void processBatch(List<ExtractedQuestionEntity> batch, String formattedCats) {
        System.out.println("Thread ID: " + Thread.currentThread().getId() + ", Name: " + Thread.currentThread().getName());
        String prompt = buildBatchPrompt(batch, formattedCats);
        String raw = chatModel.chat(prompt);
        String json = Utils.extractJsonArray(raw);
        Map<Integer, List<Long>> map = parseBatchResult(json);

        for (int idx = 0; idx < batch.size(); idx++) {
            ExtractedQuestionEntity detached = batch.get(idx);
            List<Long> chosen = map.get(idx + 1);
            String questionText = detached.getQuestionText();

            if (chosen == null || chosen.isEmpty()) {
                System.out.println("原问题: " + questionText + " -> 未分到任何类别");
                continue;
            }

            ExtractedQuestionEntity q = questionRepo.findById(detached.getId())
                    .orElseThrow(() -> new IllegalStateException("找不到问题实体 id=" + detached.getId()));

            Set<CategoryEntity> cats = q.getCategories();
            List<String> names = new ArrayList<>();
            for (Long catId : chosen) {
                categoryRepo.findById(catId).ifPresent(c -> {
                    cats.add(c);
                    names.add(c.getName());
                });
            }
            q.setCategorized(true);
            questionRepo.save(q);

            // 打印原问题及分类结果
            System.out.println("原问题: " + questionText + " -> 分类: " + String.join(", ", names));
        }
    }
    /**
     * 构建每批题目的 LLM Prompt
     * @param batch 当前题目子列表
     * @param formattedCats 已格式化的所有类别列表字符串
     * @return 替换完占位符后的完整 Prompt 文本
     */
    private String buildBatchPrompt(List<ExtractedQuestionEntity> batch, String formattedCats) {
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
    private String formatCategories(List<CategoryEntity> categories) {
        return categories.stream()
                .map(c -> c.getId() + ". " + c.getName() + "（" + c.getDescription() + "）")
                .collect(Collectors.joining("\n"));
    }

    /**
     * 内部使用的批量分类结果条目类
     */
    private static class BatchItem {
        private int index;             // 题目在本批次中的序号
        private List<Long> categories; // LLM 返回的类别 ID 列表

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public List<Long> getCategories() { return categories; }
        public void setCategories(List<Long> categories) { this.categories = categories; }
    }
}
