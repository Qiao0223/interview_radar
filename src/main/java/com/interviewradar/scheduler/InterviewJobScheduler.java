package com.interviewradar.scheduler;

import com.interviewradar.service.InterviewProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InterviewJobScheduler {
    @Autowired
    private InterviewProcessingService processingService;

    @Scheduled(cron = "0 30 0 * * *")
    public void startJob() {
        processingService.startWindow();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void stopJob() {
        processingService.stopWindow();
    }
}
