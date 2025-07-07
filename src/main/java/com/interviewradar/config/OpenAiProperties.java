package com.interviewradar.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    /** openai.api-key */
    private String apiKey;

    /** openai.model */
    private String model;

    private String baseUrl;

}
