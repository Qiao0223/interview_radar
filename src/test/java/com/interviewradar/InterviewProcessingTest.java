package com.interviewradar;

import com.interviewradar.model.repository.RawInterviewRepository;
import com.interviewradar.service.RawInterviewProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InterviewProcessingTest {

    @Autowired
    private RawInterviewProcessingService processingService;

    @Test
    void stopWindow_shouldBlockFurtherSubmissions() throws InterruptedException {
        // 先关闭处理窗口
        processingService.stopWindow();

        // 再次触发
        processingService.startWindow();
    }
}
