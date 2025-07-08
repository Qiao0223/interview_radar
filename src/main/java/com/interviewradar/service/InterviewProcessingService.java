package com.interviewradar.service;

import com.interviewradar.model.entity.InterviewEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.model.repository.InterviewRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class InterviewProcessingService {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final InterviewRepository interviewRepo;
    private final ExtractedQuestionRepository questionRepo;
    private final QuestionExtractionService extractionService;
    private final ClassificationService classificationService;
    private final Semaphore extractSemaphore;
    private final Semaphore classifySemaphore;
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);

    public InterviewProcessingService(
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            InterviewRepository interviewRepo,
            ExtractedQuestionRepository questionRepo,
            QuestionExtractionService extractionService,
            ClassificationService classificationService,
            @Value("${concurrency.extract-tasks}") int maxExtract,
            @Value("${concurrency.classify-tasks}") int maxClassify
    ) {
        this.taskExecutor = taskExecutor;
        this.interviewRepo = interviewRepo;
        this.questionRepo = questionRepo;
        this.extractionService = extractionService;
        this.classificationService = classificationService;
        this.extractSemaphore = new Semaphore(maxExtract);
        this.classifySemaphore = new Semaphore(maxClassify);
    }

    /**
     * 时窗开启：先并发提取所有面经，提取完成后再并发分类
     */
    public void startWindow() {
        windowOpen.set(true);
        List<InterviewEntity> pending = interviewRepo.findAll().stream()
                .filter(iv -> !iv.isQuestionsExtracted())
                .collect(Collectors.toList());

        // 提取阶段：并发受限
        List<CompletableFuture<Void>> extractFuts = pending.stream()
                .map(this::submitExtraction)
                .collect(Collectors.toList());

        // 全部提取完成后触发分类阶段
        CompletableFuture.allOf(extractFuts.toArray(new CompletableFuture[0]))
                .thenRunAsync(this::runClassification, taskExecutor);
    }

    /** 时窗关闭：停止新的阶段提交 */
    public void stopWindow() {
        windowOpen.set(false);
    }

    /** 提交单条提取任务 */
    private CompletableFuture<Void> submitExtraction(InterviewEntity iv) {
        if (!windowOpen.get()) return CompletableFuture.completedFuture(null);
        try {
            extractSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                extractionService.extractAndSave(iv);
            } finally {
                extractSemaphore.release();
            }
        }, taskExecutor);
    }

    /** 分类阶段入口，受限于 classifySemaphore */
    private void runClassification() {
        if (!windowOpen.get()) return;
        List<ExtractedQuestionEntity> toClassify = questionRepo.findAll().stream()
                .filter(q -> !q.isClassified())
                .collect(Collectors.toList());

        if (toClassify.isEmpty()) return;

        try {
            classifySemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // 批量并发分类，也可拆分为多批次
        CompletableFuture.runAsync(() -> {
            try {
                classificationService.classifyBatch(toClassify);
            } finally {
                classifySemaphore.release();
            }
        }, taskExecutor);
    }
}
