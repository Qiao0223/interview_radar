package com.interviewradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewradar.common.Utils;
import com.interviewradar.llm.PromptContext;
import com.interviewradar.llm.PromptTemplate;
import com.interviewradar.milvus.MilvusSearchHelper;
import com.interviewradar.model.dto.CandidateDecisionDTO;
import com.interviewradar.model.dto.ScoredStandardDTO;
import com.interviewradar.model.entity.*;
import com.interviewradar.model.enums.*;
import com.interviewradar.model.repository.StandardQuestionCategoryRepository;
import com.interviewradar.model.repository.StandardQuestionRepository;
import com.interviewradar.model.repository.StandardizationCandidateRepository;
import com.interviewradar.model.repository.RawToStandardMapRepository;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class StandardizationJudgementService {

    private final StandardizationCandidateRepository candidateRepo;
    private final StandardQuestionRepository questionRepo;
    private final RawToStandardMapRepository mapRepo;
    private final MilvusSearchHelper milvusSearchHelper;
    private final ChatModel chatModel;
    private final StandardQuestionCategoryRepository sqCatRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${milvus.threshold:0}")
    private double threshold;

    public StandardizationJudgementService(StandardizationCandidateRepository candidateRepo,
                                           StandardQuestionRepository questionRepo,
                                           RawToStandardMapRepository mapRepo,
                                           MilvusSearchHelper milvusSearchHelper,
                                           @Qualifier("deepseekChatModel")ChatModel chatModel,
                                           StandardQuestionCategoryRepository sqCatRepo) {
        this.candidateRepo = candidateRepo;
        this.questionRepo = questionRepo;
        this.mapRepo = mapRepo;
        this.milvusSearchHelper = milvusSearchHelper;
        this.chatModel = chatModel;
        this.sqCatRepo = sqCatRepo;
    }

    /**
     * 执行所有待 LLM 决策的候选
     */
    public void judgeAll() {
        var pending = candidateRepo.findByDecisionStatus(CandidateDecisionStatus.PENDING);
        pending.forEach(this::processSingle);
    }

    /**
     * 单条候选执行 LLM 决策，并设置决策状态
     */
    @Transactional
    public void processSingle(StandardizationCandidate cand) {
        Long candId = cand.getId();
        cand = candidateRepo.findById(candId).orElseThrow();

        // 解析 embedding
        float[] vector;
        try {
            vector = objectMapper.readValue(cand.getEmbedding(), float[].class);
        } catch (Exception e) {
            throw new RuntimeException("解析 embedding 失败, id=" + candId, e);
        }

        // Top-K 检索
        var idScores = milvusSearchHelper.search(vector);
        List<ScoredStandardDTO> standards = idScores.stream()
                .map(scorePair -> {
                    String status = "";
                    try {
                        Object val = scorePair.get("status");
                        status = val != null ? val.toString() : "";
                    } catch (Exception e) {
                        throw new RuntimeException("获取 status 失败", e);
                    }
                    return new ScoredStandardDTO(
                            scorePair.getLongID(),
                            scorePair.getScore(),
                            status
                    );
                })
                .collect(Collectors.toList());

        // 构造对话
        PromptContext.add(TaskType.JUDGEMENT, candId);

        // 构造 Prompt 并调用 LLM
        String prompt = buildDecisionPrompt(cand.getCandidateText(), standards);
        String rawResp = chatModel.chat(prompt);
        String json = Utils.extractJson(rawResp);

        CandidateDecisionDTO decision;
        try {
            decision = objectMapper.readValue(json, CandidateDecisionDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("解析 LLM 决策失败: " + json, e);
        }

        // 根据决策应用
        applyDecision(cand, decision);
    }

    /**
     * 构造 LLM 决策 Prompt
     */
    private String buildDecisionPrompt(String question, List<ScoredStandardDTO> candidates) {
        String template = PromptTemplate.CANDIDATE_DECISION.getTemplate();
        String candidatesBlock = IntStream.range(0, candidates.size())
                .mapToObj(i -> {
                    ScoredStandardDTO c = candidates.get(i);
                    String text = questionRepo.findById(c.getId())
                            .map(StandardQuestion::getQuestionText)
                            .orElse("未知标准问法");
                    return String.format("%d. “%s” (Id=%d)", i + 1, text, c.getId());
                })
                .collect(Collectors.joining("\n"));
        return template
                .replace("${question}", question)
                .replace("${standards}", candidatesBlock);
    }

    /**
     * 根据 LLM 决策应用不同逻辑，并设置相应状态
     */
    @Transactional
    public void applyDecision(StandardizationCandidate cand, CandidateDecisionDTO dec) {
        switch (dec.getAction()) {
            case REUSE -> applyReuseDecision(cand, dec.getChosenId());
            case CREATE -> applyCreateDecision(cand);
            case SKIP -> {
                cand.setDecisionStatus(CandidateDecisionStatus.SKIP);
                cand.setPromotionStatus(CandidatePromotionStatus.SKIPPED);
            }
        }
        cand.setReviewStatus(CandidateReviewStatus.PENDING);
        candidateRepo.save(cand);
    }

    /**
     * 应用 REUSE 决策：更新 usageCount、建立映射、同步分类
     */
    private void applyReuseDecision(StandardizationCandidate cand, Long chosenId) {
        var sq = questionRepo.getReferenceById(chosenId);
        sq.setUsageCount(sq.getUsageCount() + 1);
        questionRepo.save(sq);

        cand.setMatchedStandard(sq);

        RawToStandardMap map = RawToStandardMap.builder()
                .id(new RawToStandardMapId(cand.getRawQuestion().getId(), sq.getId()))
                .rawQuestion(cand.getRawQuestion())
                .standardQuestion(sq)
                .build();
        mapRepo.save(map);

        List<StandardQuestionCategory> joins = sqCatRepo.findByStandardQuestionId(sq.getId());
        Set<Category> standardCats = joins.stream()
                .map(StandardQuestionCategory::getCategory)
                .collect(Collectors.toSet());

        Set<Category> rawCats = cand.getRawQuestion().getCategories();
        rawCats.stream()
                .filter(cat -> !standardCats.contains(cat))
                .forEach(cat -> {
                    var id = new StandardQuestionCategoryId(sq.getId(), cat.getId());
                    var join = new StandardQuestionCategory();
                    join.setId(id);
                    join.setStandardQuestion(sq);
                    join.setCategory(cat);
                    sqCatRepo.save(join);
                });

        cand.setDecisionStatus(CandidateDecisionStatus.REUSE);
        cand.setReviewStatus(CandidateReviewStatus.PENDING);
        cand.setPromotionStatus(CandidatePromotionStatus.MERGED);
    }

    /**
     * 处理 CREATE 决策：创建新标准问法并设置状态
     */
    private void applyCreateDecision(StandardizationCandidate cand) {
        // 1. 新建标准问法
        StandardQuestion sq = new StandardQuestion();
        sq.setQuestionText(cand.getCandidateText());
        sq.setStatus(StandardStatus.PENDING);
        sq.setUsageCount(1);
        sq.setCreator("llm");
        sq.setCreatedAt(LocalDateTime.now());
        sq.setUpdatedAt(LocalDateTime.now());
        sq.setEmbedding(cand.getEmbedding());

        // 1 保存——第 1 次持久化会同时插入标准问法和中间表
        questionRepo.save(sq);

        // 2. 把向量插入 Milvus（不影响关系库）
        float[] vec;
        try {
            vec = objectMapper.readValue(cand.getEmbedding(), float[].class);
        } catch (Exception e) {
            throw new RuntimeException("解析新向量失败 id=" + sq.getId(), e);
        }
        milvusSearchHelper.insertStandardQuestion(sq.getId(), vec, sq.getQuestionText(), 0);

        // 3. 把 raw→standard 的映射也入库（使用 Builder 确保 mappedAt 非空）
        RawToStandardMap map = RawToStandardMap.builder()
                .id(new RawToStandardMapId(cand.getRawQuestion().getId(), sq.getId()))
                .rawQuestion(cand.getRawQuestion())
                .standardQuestion(sq)
                .build();
        mapRepo.save(map);

        // 4. 分类继承到 join 表
        for (Category cat : cand.getRawQuestion().getCategories()) {
            StandardQuestionCategoryId id = new StandardQuestionCategoryId(sq.getId(), cat.getId());
            StandardQuestionCategory join = new StandardQuestionCategory();
            join.setId(id);
            join.setStandardQuestion(sq);
            join.setCategory(cat);
            sqCatRepo.save(join);
        }

        // 5. 更新 candidate 的三个状态
        cand.setDecisionStatus(CandidateDecisionStatus.CREATE);
        cand.setReviewStatus(CandidateReviewStatus.PENDING);
        cand.setPromotionStatus(CandidatePromotionStatus.PROMOTED);
        candidateRepo.save(cand);
    }
}
