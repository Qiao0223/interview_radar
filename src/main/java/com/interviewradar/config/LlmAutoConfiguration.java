package com.interviewradar.config;

import com.interviewradar.llm.LanguageModel;
import com.interviewradar.llm.OpenAiLanguageModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
        LlmProperties.class,
        OpenAiProperties.class,
        AliyunProperties.class,
        DeepseekProperties.class
})
public class LlmAutoConfiguration {

    private final LlmProperties llmProps;
    private final OpenAiProperties openAiProps;
    private final AliyunProperties aliyunProps;
    private final DeepseekProperties deepseekProps;

    public LlmAutoConfiguration(LlmProperties llmProps,
                                OpenAiProperties openAiProps,
                                AliyunProperties aliyunProps,
                                DeepseekProperties deepseekProps) {
        this.llmProps = llmProps;
        this.openAiProps = openAiProps;
        this.aliyunProps = aliyunProps;
        this.deepseekProps = deepseekProps;
    }

    @Bean
    public LanguageModel languageModel(RestTemplate rest) {
        String provider = llmProps.getProvider().toLowerCase();
        switch (provider) {
            case "openai":
                return new OpenAiLanguageModel(
                        openAiProps.getApiKey(),
                        openAiProps.getModel(),
                        openAiProps.getBaseUrl()
                );
            case "aliyun":
                return new OpenAiLanguageModel(
                        aliyunProps.getApiKey(),
                        aliyunProps.getModel(),
                        aliyunProps.getBaseUrl()
                );
            case "deepseek":
                return new OpenAiLanguageModel(
                        deepseekProps.getApiKey(),
                        deepseekProps.getModel(),
                        deepseekProps.getBaseUrl()
                );
            default:
                throw new IllegalArgumentException("未知的 llm.provider: " + llmProps.getProvider());
        }
    }
}
