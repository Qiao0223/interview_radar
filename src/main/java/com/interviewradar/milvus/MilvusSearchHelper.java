package com.interviewradar.milvus;

import com.interviewradar.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.R;
import io.milvus.param.MetricType;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.SearchParam;
import io.milvus.grpc.SearchResults;
import io.milvus.response.SearchResultsWrapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Service
public class MilvusSearchHelper {

    private final MilvusServiceClient milvusClient;
    private final int topK;
    private final int nprobe;

    public MilvusSearchHelper(MilvusServiceClient milvusClient, MilvusProperties properties) {
        this.milvusClient = milvusClient;
        this.topK = properties.getTopK();
        this.nprobe = properties.getNprob();
    }

    public List<SearchResultsWrapper.IDScore> search(float[] vector) {
        List<Float> vecList = toList(vector);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName("standard_question")
                .withMetricType(MetricType.L2)
                .withTopK(topK)
                .withVectorFieldName("embedding")
                .withFloatVectors(Collections.singletonList(vecList))
                .withParams("{\"nprobe\":" + nprobe + "}")
                .withOutFields(Arrays.asList("id", "question_text", "status"))
                .build();

        R<SearchResults> resp = milvusClient.search(searchParam);
        if (resp.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Milvus search failed: " + resp.getMessage());
        }

        SearchResults grpcResults = resp.getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(grpcResults.getResults());
        return wrapper.getIDScore(0);
    }

    /**
     * Insert a new record into the standard_question collection.
     */
    public void insertStandardQuestion(Long id, float[] vector, String questionText, int status) {
        List<Float> vecList = toList(vector);
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName("standard_question")
                .withFields(List.of(
                        new Field("id", Collections.singletonList(id)),
                        new Field("question_text", Collections.singletonList(questionText)),
                        new Field("embedding", Collections.singletonList(vecList)),
                        new Field("status", Collections.singletonList(status))
                ))
                .build();
        milvusClient.insert(insertParam);
    }

    private List<Float> toList(float[] vector) {
        return java.util.stream.IntStream.range(0, vector.length)
                .mapToObj(i -> vector[i])
                .collect(Collectors.toList());
    }
}