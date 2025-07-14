package com.interviewradar.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Service
public class RawInterviewProcessingService {

    private final RawQuestionExtractionService extractionService;
    private final RawQuestionClassificationService classificationService;
    private final CrawlerService crawlerService;
    private final RawQuestionStandardizationService standardizationService;
    private final StandardizationJudgementService judgementService;

    AtomicBoolean windowOpen = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(RawInterviewProcessingService.class);


    public void startWindow(){
        // 打开窗口
        windowOpen.set(true);

        // 从牛客爬取面经原文
        log.info("开始爬虫任务");
        crawlerService.crawlNewInterviews();

        // 从面经原文提取问题
        log.info("开始提取问题任务");
        extractionService.extractAllInterviews();

        // 提取完成后，直接同步触发分类流程
        log.info("开始问题分类任务");
        classificationService.classifyAllRawQuestions();

        // 将分类好的 raw question 生成标准问题
        log.info("开始标准化任务");
        standardizationService.standardizeAll();

        log.info("开始 LLM 决策任务");
        judgementService.judgeAll();

    }

    public void stopWindow() {
        windowOpen.set(false);
    }
}
