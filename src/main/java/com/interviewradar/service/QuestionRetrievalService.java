package com.interviewradar.service;

import com.interviewradar.model.dto.CanonicalQuestionCandidateDTO;
import com.interviewradar.model.entity.CanonicalQuestionEntity;
import com.interviewradar.model.entity.CategoryEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.repository.CanonicalQuestionRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for embedding an extracted question and retrieving top-K candidates from Milvus.
 */
@Service
public class QuestionRetrievalService {

    private static final int TOP_K = 5;

    private final ExtractedQuestionRepository questionRepo;
    private final CanonicalQuestionRepository canonicalRepo;
    private final EmbeddingModel embeddingModel;
    private final MilvusServiceClient milvusClient;

    public QuestionRetrievalService(
            ExtractedQuestionRepository questionRepo,
            CanonicalQuestionRepository canonicalRepo,
            EmbeddingModel embeddingModel,
            MilvusServiceClient milvusClient
    ) {
        this.questionRepo = questionRepo;
        this.canonicalRepo = canonicalRepo;
        this.embeddingModel = embeddingModel;
        this.milvusClient = milvusClient;
    }

    /**
     * Retrieve top-K canonical question candidates for the given extracted question.
     */
    public List<CanonicalQuestionCandidateDTO> retrieveCandidates(Long extractedQuestionId) {
        // 1. Load question and category
        ExtractedQuestionEntity question = questionRepo.findById(extractedQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + extractedQuestionId));
        CategoryEntity category = question.getCategories().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question not categorized: " + extractedQuestionId));
        long categoryId = category.getId();

        // 2. Compute embedding
        Response<Embedding> resp = embeddingModel.embed(question.getQuestionText());
        float[] vector = resp.content().vector();

        // 3. Build search parameters
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName("canonical_question")
                .withMetricType(MetricType.L2)
                .withTopK(TOP_K)
                .withExpr("category_id == " + categoryId)
                .withFloatVectors(Collections.singletonList(toList(vector)))
                .build();

        // 4. Execute search
        R<SearchResults> r = milvusClient.search(searchParam);
        SearchResults sr = r.getData();

        // 5. Parse results via wrapper
        SearchResultsWrapper wrapper = new SearchResultsWrapper(sr.getResults());
        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);

        // 6. Map to Candidate DTOs
        List<CanonicalQuestionCandidateDTO> candidates = new ArrayList<>();
        for (SearchResultsWrapper.IDScore scorePair : idScores) {
            long cqId = scorePair.getLongID();
            float score = scorePair.getScore();
            Optional<CanonicalQuestionEntity> opt = canonicalRepo.findById(cqId);
            opt.ifPresent(cq -> candidates.add(
                    new CanonicalQuestionCandidateDTO(cq.getId(), cq.getText(), cq.getStatus(), score)
            ));
        }
        return candidates;
    }

    private List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float v : vector) {
            list.add(v);
        }
        return list;
    }
}