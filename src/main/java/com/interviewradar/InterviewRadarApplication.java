package com.interviewradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import dev.langchain4j.openai.spring.AutoConfig;

@EnableConfigurationProperties
@SpringBootApplication(exclude = {AutoConfig.class})
@ConfigurationPropertiesScan("com.interviewradar.config")
public class InterviewRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewRadarApplication.class, args);
    }

}
