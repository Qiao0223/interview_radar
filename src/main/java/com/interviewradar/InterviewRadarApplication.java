package com.interviewradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
@ConfigurationPropertiesScan("com.interviewradar.config")
public class InterviewRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewRadarApplication.class, args);
    }

}
