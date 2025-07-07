package com.interviewradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.common.Utils;
import com.interviewradar.config.ClassificationProperties;
import com.interviewradar.data.entity.CategoryEntity;
import com.interviewradar.data.entity.QuestionEntity;
import com.interviewradar.data.repository.CategoryRepository;
import com.interviewradar.data.repository.QuestionRepository;
import com.interviewradar.llm.LanguageModel;
import com.interviewradar.llm.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分类服务：调用 LLM 将题目按类别进行批量分类
 */
@Service
public class ClassificationService {
    private final LanguageModel llm;                  // LLM 接口，用于生成分类提示
    private final CategoryRepository categoryRepo;    // 类别仓库，读取所有可用类别
    private final QuestionRepository questionRepo;    // 题目仓库，用于保存分类结果
    private final ClassificationProperties props;     // batchSize 配置
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 对象映射器，用于解析 LLM 返回的 JSON

    public ClassificationService(LanguageModel llm,
                                 CategoryRepository categoryRepo,
                                 QuestionRepository questionRepo,
                                 ClassificationProperties props) {
        this.llm = llm;
        this.categoryRepo = categoryRepo;
        this.questionRepo = questionRepo;
        this.props = props;
    }

    /**
     * 批量分类，一次请求多条，批次大小从配置读取
     */
    public void classifyBatch(List<QuestionEntity> questions) {
        int batchSize = props.getBatchSize();
        List<CategoryEntity> allCats = categoryRepo.findAll();
        String formattedCats = formatCategories(allCats);

        for (int i = 0; i < questions.size(); i += batchSize) {
            List<QuestionEntity> batch = questions.subList(i, Math.min(i + batchSize, questions.size()));
            processBatch(batch, formattedCats);
        }
    }

    /**
     * 单独事务，分批提交
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<QuestionEntity> batch, String formattedCats) {
        String prompt = buildBatchPrompt(batch, formattedCats);
        String raw = llm.generate(prompt);
        String json = Utils.extractJson(raw);
        Map<Integer, List<Long>> map = parseBatchResult(json);

        for (int idx = 0; idx < batch.size(); idx++) {
            QuestionEntity q = batch.get(idx);
            int index = idx + 1;
            List<Long> chosen = map.get(index);
            if (chosen == null || chosen.isEmpty()) {
                continue;
            }

            Set<CategoryEntity> cats = q.getCategories();
            for (Long catId : chosen) {
                categoryRepo.findById(catId).ifPresent(cats::add);
            }
            q.setClassified(true);
            questionRepo.save(q);
        }
    }
    /**
     * 构建每批题目的 LLM Prompt
     * @param batch 当前题目子列表
     * @param formattedCats 已格式化的所有类别列表字符串
     * @return 替换完占位符后的完整 Prompt 文本
     */
    private String buildBatchPrompt(List<QuestionEntity> batch, String formattedCats) {
        // 获取原始模板
        String template = PromptTemplate.QUESTION_CLASSIFICATION.getTemplate();
        // 替换批次数量与分类列表
        template = template.replace("${n}", String.valueOf(batch.size()))
                .replace("{categories}", formattedCats);
        // 截取到“问题列表：”这一行，后续自定义题目列表
        String header = "问题列表：";
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
