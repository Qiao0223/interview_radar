package com.interviewradar.service;

import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.model.repository.RawInterviewRepository;
import lombok.Data;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Service
public class RawInterviewProcessingService {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final RawInterviewRepository interviewRepo;
    private final RawQuestionRepository questionRepo;
    private final RawQuestionExtractionService extractionService;
    private final RawQuestionClassificationService classificationService;
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);

    private final AtomicBoolean extractionStarted = new AtomicBoolean(false);
    private final AtomicBoolean classificationStarted = new AtomicBoolean(false);

    public void startWindow() {
        windowOpen.set(true);

        if (extractionStarted.compareAndSet(false, true)) {
            System.out.println("[INFO] 开始提取问题任务");
        }

        // 使用 repository 查询未提取的问题
        List<RawInterview> pending = interviewRepo.findByQuestionsExtractedFalse();

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

        List<RawQuestion> toClassify = questionRepo.findByCategoriesAssignedFalse();

        if (toClassify.isEmpty()) return;

        CompletableFuture.runAsync(
                () -> classificationService.classifyBatch(toClassify),
                taskExecutor
        );
    }
}