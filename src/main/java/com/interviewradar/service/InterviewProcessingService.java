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

    private final AtomicBoolean extractionStarted = new AtomicBoolean(false);
    private final AtomicBoolean classificationStarted = new AtomicBoolean(false);

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

    public void startWindow() {
        windowOpen.set(true);

        if (extractionStarted.compareAndSet(false, true)) {
            System.out.println("[INFO] 开始提取问题任务");
        }

        // 使用 repository 查询未提取的问题
        List<InterviewEntity> pending = interviewRepo.findByQuestionsExtractedFalse();

        List<CompletableFuture<Void>> extractFuts = pending.stream()
                .map(iv -> CompletableFuture.runAsync(
                        () -> extractionService.extractAndSave(iv),
                        taskExecutor
                ))
                .toList();

        CompletableFuture.allOf(extractFuts.toArray(new CompletableFuture[0]))
                .thenRunAsync(this::runClassification, taskExecutor);
    }

    public void stopWindow() {
        windowOpen.set(false);
        extractionStarted.set(false);
        classificationStarted.set(false);
    }

    private void runClassification() {
        if (!windowOpen.get()) return;

        if (classificationStarted.compareAndSet(false, true)) {
            System.out.println("[INFO] 开始分类问题任务");
        }

        List<ExtractedQuestionEntity> toClassify = questionRepo.findByCategorizedFalse();

        if (toClassify.isEmpty()) return;

        CompletableFuture.runAsync(
                () -> classificationService.classifyBatch(toClassify),
                taskExecutor
        );
    }
}