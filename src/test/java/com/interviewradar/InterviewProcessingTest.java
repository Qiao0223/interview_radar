package com.interviewradar;

import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.model.repository.RawInterviewRepository;
import com.interviewradar.service.RawInterviewProcessingService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource
class InterviewProcessingTest {

    @Autowired
    private RawInterviewProcessingService processingService;

    @Autowired
    private RawInterviewRepository interviewRepo;

    @Autowired
    private RawQuestionRepository questionRepo;

    private Set<Long> pendingIds;

    @BeforeEach
    void setUp() {
        // 取出所有待处理面经的 ID
        pendingIds = interviewRepo.findAll().stream()
                .filter(iv -> !iv.getQuestionsExtracted())
                .map(RawInterview::getId)
                .collect(Collectors.toSet());
    }

    @Test
    void startWindow_shouldProcessDBInterviewsFully() {
        // 手动触发流程
        processingService.startWindow();

        // 等待所有待处理面经完成抽取
        Awaitility.await()
                .atMost(Duration.ofMinutes(20))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<RawInterview> remaining = interviewRepo.findAll().stream()
                            .filter(iv -> pendingIds.contains(iv.getId()) && !iv.getQuestionsExtracted())
                            .collect(Collectors.toList());
                    assertThat(remaining).isEmpty();
                });
    }

    @Test
    void stopWindow_shouldBlockFurtherSubmissions() {
        // 先关闭处理窗口
        processingService.stopWindow();

        // 再次触发
        processingService.startWindow();

        // 确认状态未变化
        interviewRepo.findAll().stream()
                .filter(iv -> pendingIds.contains(iv.getId()))
                .forEach(iv -> assertThat(iv.getQuestionsExtracted()).isFalse());
    }
}
