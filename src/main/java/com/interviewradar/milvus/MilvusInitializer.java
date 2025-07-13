package com.interviewradar.milvus;

import com.interviewradar.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MilvusInitializer {

    private final MilvusServiceClient milvusClient;
    private final MilvusProperties properties;

    @PostConstruct
    public void initCollections() {
        createStandardQuestionCollection();
    }

    private void createStandardQuestionCollection() {
        R<Boolean> exists = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName("standard_question").build()
        );
        if (Boolean.TRUE.equals(exists.getData())) return;

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName("standard_question")
                        .withFieldTypes(Arrays.asList(
                                FieldType.newBuilder().withName("id").withDataType(DataType.Int64).withPrimaryKey(true).withAutoID(false).build(),
                                FieldType.newBuilder().withName("question_text").withDataType(DataType.VarChar).withMaxLength(256).build(),
                                FieldType.newBuilder().withName("embedding").withDataType(DataType.FloatVector).withDimension(properties.getEmbeddingDim()).build(),
                                FieldType.newBuilder().withName("status").withDataType(DataType.Int32).build()
                        ))
                        .build()
        );
    }
}
