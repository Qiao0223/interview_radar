package com.interviewradar.milvus;

import com.interviewradar.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.collection.LoadCollectionParam;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MilvusInitializer {

    private final MilvusServiceClient milvusClient;
    private final MilvusProperties properties;
    private static final String COLLECTION_NAME = "standard_question";
    private static final String EMBEDDING_FIELD = "embedding";

    @PostConstruct
    public void initCollections() {
        createStandardQuestionCollection();
        createEmbeddingIndex();
        loadCollection();
    }

    private void createStandardQuestionCollection() {
        R<Boolean> exists = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
        if (Boolean.TRUE.equals(exists.getData())) {
            return;
        }

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .withFieldTypes(Arrays.asList(
                                FieldType.newBuilder()
                                        .withName("id")
                                        .withDataType(DataType.Int64)
                                        .withPrimaryKey(true)
                                        .withAutoID(false)
                                        .build(),
                                FieldType.newBuilder()
                                        .withName("question_text")
                                        .withDataType(DataType.VarChar)
                                        .withMaxLength(256)
                                        .build(),
                                FieldType.newBuilder()
                                        .withName(EMBEDDING_FIELD)
                                        .withDataType(DataType.FloatVector)
                                        .withDimension(properties.getEmbeddingDim())
                                        .build(),
                                FieldType.newBuilder()
                                        .withName("status")
                                        .withDataType(DataType.Int32)
                                        .build()
                        ))
                        .build()
        );
    }

    private void createEmbeddingIndex() {
        // build index with JSON string param since builder.withParams is not available
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFieldName(EMBEDDING_FIELD)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\":128}")
                .build();

        milvusClient.createIndex(indexParam);
    }

    private void loadCollection() {
        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
    }
}
