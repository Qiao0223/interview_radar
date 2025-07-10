package com.interviewradar.milvus;

import com.interviewradar.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MilvusInitializer {

    private final MilvusServiceClient milvusClient;
    private final MilvusProperties properties;

    @PostConstruct
    public void initCollections() {
        createCanonicalQuestionCollection();
        createTopicChunkCollection();
    }

    private void createCanonicalQuestionCollection() {
        R<Boolean> exists = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName("canonical_question").build()
        );
        if (Boolean.TRUE.equals(exists.getData())) return;

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName("canonical_question")
                        .withFieldTypes(Arrays.asList(
                                FieldType.newBuilder().withName("id").withDataType(DataType.Int64).withPrimaryKey(true).withAutoID(false).build(),
                                FieldType.newBuilder().withName("text").withDataType(DataType.VarChar).withMaxLength(256).build(),
                                FieldType.newBuilder().withName("question_embedding").withDataType(DataType.FloatVector).withDimension(properties.getEmbeddingDim()).build(),
                                FieldType.newBuilder().withName("category_id").withDataType(DataType.Int64).build(),
                                FieldType.newBuilder().withName("status").withDataType(DataType.Int32).build()
                        ))
                        .build()
        );
    }

    private void createTopicChunkCollection() {
        R<Boolean> exists = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName("topic_chunk").build()
        );
        if (Boolean.TRUE.equals(exists.getData())) return;

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName("topic_chunk")
                        .withFieldTypes(Arrays.asList(
                                FieldType.newBuilder().withName("chunk_id").withDataType(DataType.Int64).withPrimaryKey(true).withAutoID(false).build(),
                                FieldType.newBuilder().withName("topic_id").withDataType(DataType.Int64).build(),
                                FieldType.newBuilder().withName("paragraph_idx").withDataType(DataType.Int32).build(),
                                FieldType.newBuilder().withName("chunk_text").withDataType(DataType.VarChar).withMaxLength(2048).build(),
                                FieldType.newBuilder().withName("chunk_embedding").withDataType(DataType.FloatVector).withDimension(properties.getEmbeddingDim()).build(),
                                FieldType.newBuilder().withName("category_id").withDataType(DataType.Int64).build(),
                                FieldType.newBuilder().withName("status").withDataType(DataType.Int32).build()
                        ))
                        .build()
        );
    }
}
