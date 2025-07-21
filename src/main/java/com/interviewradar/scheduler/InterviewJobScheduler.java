package com.interviewradar.scheduler;

import com.interviewradar.service.RawInterviewProcessingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class InterviewJobScheduler {

    @Autowired
    private RawInterviewProcessingService processingService;

    // 每天 00:30 执行一次，开启窗口
    @Scheduled(cron = "0 30 0 * * *")
    public void startJob() {
        processingService.startWindow();
    }

    // 每天 08:00 执行一次，关闭窗口
    @Scheduled(cron = "0 0 8 * * *")
    public void stopJob() {
        processingService.stopWindow();
    }

    // 启动时判断是否在窗口期，如果是则执行一次
    @PostConstruct
    public void checkStartupWindow() {
        // 测试时强制开启窗口
        processingService.startWindow();


        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(0, 30);
        LocalTime end = LocalTime.of(8, 0);

        if (!now.isBefore(start) && now.isBefore(end)) {
            processingService.startWindow();
        }
    }
}
