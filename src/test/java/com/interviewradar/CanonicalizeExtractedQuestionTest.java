package com.interviewradar;

import com.interviewradar.model.entity.CandidateCanonicalQuestionEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.repository.CandidateCanonicalQuestionRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.service.CanonicalizeExtractedQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CanonicalizeExtractedQuestionTest {

    @Autowired
    private ExtractedQuestionRepository extractedRepo;

    @Autowired
    private CandidateCanonicalQuestionRepository candidateRepo;

    @Autowired
    private CanonicalizeExtractedQuestionService service;

    @Test
    public void testBatchStandardizeExistingData() {
        // 1. 从数据库读取提取问题
        List<ExtractedQuestionEntity> extracted = extractedRepo.findByCanonicalizedFalseAndCategorizedTrue();

        service.batchStandardize(extracted);
    }
}
