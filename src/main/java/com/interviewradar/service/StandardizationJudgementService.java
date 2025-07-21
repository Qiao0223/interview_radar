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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${milvus.threshold:0}")
    private double threshold;
    private static final Logger log = LoggerFactory.getLogger(StandardizationJudgementService.class);

    @Lazy
    @Autowired
    private StandardizationJudgementService self;

    @Autowired
    public StandardizationJudgementService(
            StandardizationCandidateRepository candidateRepo,
            StandardQuestionRepository questionRepo,
            RawToStandardMapRepository mapRepo,
            MilvusSearchHelper milvusSearchHelper,
            @Qualifier("deepseekChatModel") ChatModel chatModel,
            StandardQuestionCategoryRepository sqCatRepo,
            ThreadPoolTaskExecutor taskExecutor
    ) {
        this.candidateRepo = candidateRepo;
        this.questionRepo = questionRepo;
        this.mapRepo = mapRepo;
        this.milvusSearchHelper = milvusSearchHelper;
        this.chatModel = chatModel;
        this.sqCatRepo = sqCatRepo;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 单线程处理所有候选
     */
    public void judgeAllSingle() {
        int page = 0;
        int size = 100;
        Page<StandardizationCandidate> pendingPage;

        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            pendingPage = candidateRepo.findByDecisionStatus(CandidateDecisionStatus.PENDING, pageRequest);
            pendingPage.forEach(self::processSingle);
            page++;
        } while (pendingPage.hasNext());
    }

    /**
     * 多线程批量处理所有候选，使用共享线程池
     */
    public void judgeAllConcurrent() {
        int page = 0;
        int size = 100;
        Page<StandardizationCandidate> pendingPage;
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            pendingPage = candidateRepo.findByDecisionStatus(CandidateDecisionStatus.PENDING, pageRequest);
            for (StandardizationCandidate cand : pendingPage) {
                futures.add(
                        CompletableFuture.runAsync(() -> {
                            try {
                                self.processSingle(cand);
                            } catch (Exception ex) {
                                // 捕获包括主键冲突在内的所有异常，记录后继续
                                log.error("处理候选 {} 时失败，已忽略", cand.getId(), ex);
                            }
                        }, taskExecutor)
                );
            }
            page++;
        } while (pendingPage.hasNext());

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    @Transactional
    public void processSingle(StandardizationCandidate cand) {
        Long candId = cand.getId();
        cand = candidateRepo.findWithRawQuestionAndCategoriesById(candId)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candId));

        // 业务计算省略（同原实现）
        float[] vector;
        try {
            vector = objectMapper.readValue(cand.getEmbedding(), float[].class);
        } catch (Exception e) {
            throw new RuntimeException("解析 embedding 失败, id=" + candId, e);
        }

        var idScores = milvusSearchHelper.search(vector);
        List<ScoredStandardDTO> standards = idScores.stream()
                .map(scorePair -> new ScoredStandardDTO(
                        scorePair.getLongID(),
                        scorePair.getScore(),
                        scorePair.get("status") != null ? scorePair.get("status").toString() : "",
                        scorePair.get("question_text") != null ? scorePair.get("question_text").toString() : ""
                ))
                .collect(Collectors.toList());

        PromptContext.add(TaskType.JUDGEMENT, candId);
        String prompt = buildDecisionPrompt(cand.getCandidateText(), standards);
        String rawResp = chatModel.chat(prompt);
        String json = Utils.extractJson(rawResp);

        log.info("候选问题: {}，检索到标准候选: {}", cand.getCandidateText(), standards);
        log.info("LLM 原始返回: {}", rawResp);

        CandidateDecisionDTO decision;
        try {
            decision = objectMapper.readValue(json, CandidateDecisionDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("解析 LLM 决策失败: " + json, e);
        }

        if (decision.getAction() == CandidateAction.REUSE) {
            Set<Long> validIds = standards.stream()
                    .map(ScoredStandardDTO::getId)
                    .collect(Collectors.toSet());
            if (!validIds.contains(decision.getChosenId())) {
                log.error("LLM 返回了无效的 ID: {}，有效 ID 列表: {}，将重试处理", decision.getChosenId(), validIds);
                return;
            }
        }

        switch (decision.getAction()) {
            case REUSE -> applyReuseDecision(cand, decision.getChosenId());
            case CREATE -> applyCreateDecision(cand);
            case SKIP -> {
                cand.setDecisionStatus(CandidateDecisionStatus.SKIP);
                cand.setPromotionStatus(CandidatePromotionStatus.SKIPPED);
            }
        }

        cand.setReviewStatus(CandidateReviewStatus.PENDING);
        candidateRepo.save(cand);

        if (decision.getAction() == CandidateAction.CREATE) {
            final long stdId = cand.getMatchedStandard().getId();
            final String embJson = cand.getEmbedding();
            final String text = cand.getMatchedStandard().getQuestionText();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        float[] vec = objectMapper.readValue(embJson, float[].class);
                        milvusSearchHelper.insertStandardQuestion(
                                stdId,
                                vec,
                                text,
                                0
                        );
                    } catch (Exception e) {
                        log.error("Milvus 插入失败", e);
                    }
                }
            });
        }
    }

    private String buildDecisionPrompt(String question, List<ScoredStandardDTO> candidates) {
        String template = PromptTemplate.CANDIDATE_DECISION.getTemplate();
        String block = IntStream.range(0, candidates.size())
                .mapToObj(i -> {
                    ScoredStandardDTO c = candidates.get(i);
                    return String.format("%d. ‘%s’ (Id=%d)", i+1, c.getQuestionText(), c.getId());
                })
                .collect(Collectors.joining("\n"));
        return template.replace("${question}", question)
                .replace("${standards}", block);
    }

    private void applyReuseDecision(StandardizationCandidate cand, Long chosenId) {
        var sq = questionRepo.getReferenceById(chosenId);
        sq.setUsageCount(sq.getUsageCount() + 1);
        questionRepo.save(sq);

        cand.setMatchedStandard(sq);

        // 只在映射不存在时才插入
        RawToStandardMapId mapId = new RawToStandardMapId(
                cand.getRawQuestion().getId(), sq.getId()
        );
        if (!mapRepo.existsById(mapId)) {
            mapRepo.save(RawToStandardMap.builder()
                    .id(mapId)
                    .rawQuestion(cand.getRawQuestion())
                    .standardQuestion(sq)
                    .build());
        } else {
            log.debug("映射 {} 已存在，跳过保存", mapId);
        }

        Set<Category> stdCats = sqCatRepo.findByStandardQuestionId(sq.getId()).stream()
                .map(StandardQuestionCategory::getCategory)
                .collect(Collectors.toSet());
        cand.getRawQuestion().getCategories().stream()
                .filter(cat -> !stdCats.contains(cat))
                .forEach(cat -> sqCatRepo.save(new StandardQuestionCategory(
                        new StandardQuestionCategoryId(sq.getId(), cat.getId()), sq, cat)));

        cand.setDecisionStatus(CandidateDecisionStatus.REUSE);
        cand.setPromotionStatus(CandidatePromotionStatus.MERGED);
    }

    private void applyCreateDecision(StandardizationCandidate cand) {
        StandardQuestion sq = new StandardQuestion();
        sq.setQuestionText(cand.getCandidateText());
        sq.setStatus(StandardStatus.PENDING);
        sq.setUsageCount(1);
        sq.setCreator("llm");
        sq.setCreatedAt(LocalDateTime.now());
        sq.setUpdatedAt(LocalDateTime.now());
        sq.setEmbedding(cand.getEmbedding());
        questionRepo.save(sq);

        // 只在映射不存在时才插入
        RawToStandardMapId mapId = new RawToStandardMapId(
                cand.getRawQuestion().getId(), sq.getId()
        );
        if (!mapRepo.existsById(mapId)) {
            mapRepo.save(RawToStandardMap.builder()
                    .id(mapId)
                    .rawQuestion(cand.getRawQuestion())
                    .standardQuestion(sq)
                    .build());
        } else {
            log.debug("映射 {} 已存在，跳过保存", mapId);
        }

        cand.getRawQuestion().getCategories().forEach(cat ->
                sqCatRepo.save(new StandardQuestionCategory(
                        new StandardQuestionCategoryId(sq.getId(), cat.getId()), sq, cat))
        );

        cand.setMatchedStandard(sq);
        cand.setDecisionStatus(CandidateDecisionStatus.CREATE);
        cand.setPromotionStatus(CandidatePromotionStatus.PROMOTED);
    }
}
