package com.interviewradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "classification")
@Data
public class ClassificationProperties {
    /**
     * 一次最多给 LLM 分类多少条问题
     */
    private int batchSize;
}

