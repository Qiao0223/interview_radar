package com.interviewradar.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    private String host = "localhost";
    private Integer port = 19530;
    private Integer embeddingDim = 1024;

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam param = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        return new MilvusServiceClient(param);
    }

    @PostConstruct
    public void initCollections() {
        MilvusServiceClient client = milvusClient();
        createCanonicalQuestionCollection(client);
        createTopicChunkCollection(client);
    }

    private void createCanonicalQuestionCollection(MilvusServiceClient client) {
        HasCollectionParam has = HasCollectionParam.newBuilder()
                .withCollectionName("canonical_question")
                .build();
        R<Boolean> exists = client.hasCollection(has);
        if (exists != null && Boolean.TRUE.equals(exists.getData())) {
            return;
        }
        FieldType id = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType text = FieldType.newBuilder()
                .withName("text")
                .withDataType(DataType.VarChar)
                .withMaxLength(256)
                .build();
        FieldType embedding = FieldType.newBuilder()
                .withName("question_embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(embeddingDim)
                .build();
        FieldType catId = FieldType.newBuilder()
                .withName("category_id")
                .withDataType(DataType.Int64)
                .build();
        FieldType status = FieldType.newBuilder()
                .withName("status")
                .withDataType(DataType.Int32)
                .build();
        CreateCollectionParam create = CreateCollectionParam.newBuilder()
                .withCollectionName("canonical_question")
                .withFieldTypes(Arrays.asList(id, text, embedding, catId, status))
                .build();
        client.createCollection(create);
    }

    private void createTopicChunkCollection(MilvusServiceClient client) {
        HasCollectionParam has = HasCollectionParam.newBuilder()
                .withCollectionName("topic_chunk")
                .build();
        R<Boolean> exists = client.hasCollection(has);
        if (exists != null && Boolean.TRUE.equals(exists.getData())) {
            return;
        }
        FieldType chunkId = FieldType.newBuilder()
                .withName("chunk_id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType topicId = FieldType.newBuilder()
                .withName("topic_id")
                .withDataType(DataType.Int64)
                .build();
        FieldType idx = FieldType.newBuilder()
                .withName("paragraph_idx")
                .withDataType(DataType.Int32)
                .build();
        FieldType text = FieldType.newBuilder()
                .withName("chunk_text")
                .withDataType(DataType.VarChar)
                .withMaxLength(2048)
                .build();
        FieldType embedding = FieldType.newBuilder()
                .withName("chunk_embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(embeddingDim)
                .build();
        FieldType catId = FieldType.newBuilder()
                .withName("category_id")
                .withDataType(DataType.Int64)
                .build();
        FieldType status = FieldType.newBuilder()
                .withName("status")
                .withDataType(DataType.Int32)
                .build();
        CreateCollectionParam create = CreateCollectionParam.newBuilder()
                .withCollectionName("topic_chunk")
                .withFieldTypes(Arrays.asList(chunkId, topicId, idx, text, embedding, catId, status))
                .build();
        client.createCollection(create);
    }
}
