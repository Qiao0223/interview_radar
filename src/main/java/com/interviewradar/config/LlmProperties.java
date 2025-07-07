package com.interviewradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    /**
     * llm.provider: openai, aliyun, deepseek
     */
    private String provider;

}

