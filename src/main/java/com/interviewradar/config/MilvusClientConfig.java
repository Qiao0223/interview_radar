package com.interviewradar.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MilvusClientConfig {

    private final MilvusProperties properties;

    @Bean
    public MilvusServiceClient milvusClient() {
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(properties.getHost())
                        .withPort(properties.getPort())
                        .build()
        );
    }
}
