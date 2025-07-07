package com.interviewradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun")
public class AliyunProperties {
    /** sk-… 开头的模型 API Key */
    private String apiKey;
    /** OpenAI 兼容的调用地址 */
    private String baseUrl;
    /** 要调用的模型名 */
    private String model;
}
