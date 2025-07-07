package com.interviewradar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync            // 开启 @Async 支持
@EnableScheduling       // 开启 @Scheduled 支持
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor(
            @Value("${threadpool.core-size:5}") int coreSize,
            @Value("${threadpool.max-size:10}") int maxSize,
            @Value("${threadpool.queue-capacity:50}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(coreSize);
        exec.setMaxPoolSize(maxSize);
        exec.setQueueCapacity(queueCapacity);
        exec.setThreadNamePrefix("LLM-");
        exec.initialize();
        return exec;
    }
}
