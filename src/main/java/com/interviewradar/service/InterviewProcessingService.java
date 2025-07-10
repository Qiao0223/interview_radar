/*
 * File: com/interviewradar/service/InterviewProcessingService.java
 */
package com.interviewradar.service;

import com.interviewradar.model.entity.InterviewEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.model.repository.InterviewRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class InterviewProcessingService {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final InterviewRepository interviewRepo;
    private final ExtractedQuestionRepository questionRepo;
    private final QuestionExtractionService extractionService;
    private final ClassificationService classificationService;
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);

    public InterviewProcessingService(
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            InterviewRepository interviewRepo,
            ExtractedQuestionRepository questionRepo,
            QuestionExtractionService extractionService,
            ClassificationService classificationService
    ) {
        this.taskExecutor = taskExecutor;
        this.interviewRepo = interviewRepo;
        this.questionRepo = questionRepo;
        this.extractionService = extractionService;
        this.classificationService = classificationService;
    }

    /**
     * 开窗：并发提取后并发分类，使用同一线程池
     */
    public void startWindow() {
        windowOpen.set(true);
        List<InterviewEntity> pending = interviewRepo.findAll().stream()
                .filter(iv -> !iv.isQuestionsExtracted())
                .toList();

        List<CompletableFuture<Void>> extractFuts = pending.stream()
                .map(iv -> CompletableFuture.runAsync(
                        () -> extractionService.extractAndSave(iv),
                        taskExecutor
                ))
                .toList();

        CompletableFuture.allOf(extractFuts.toArray(new CompletableFuture[0]))
                .thenRunAsync(this::runClassification, taskExecutor);
    }

    /**
     * 关闭新任务提交
     */
    public void stopWindow() {
        windowOpen.set(false);
    }

    /**
     * 并发分类所有待分类问题
     */
    private void runClassification() {
//        if (!windowOpen.get()) return;
//
//        List<ExtractedQuestionEntity> toClassify = questionRepo.findAll().stream()
//                .filter(q -> !q.getCategorized())
//                .collect(Collectors.toList());
//
//        if (toClassify.isEmpty()) return;
//
//        CompletableFuture.runAsync(
//                () -> classificationService.classifyBatch(toClassify),
//                taskExecutor
//        );
    }
}
