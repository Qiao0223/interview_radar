package com.interviewradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {
    private String host;
    private Integer port;
    private Integer embeddingDim;
    private Integer topK;
    private Integer nprob;
    private Double threshold;
}
