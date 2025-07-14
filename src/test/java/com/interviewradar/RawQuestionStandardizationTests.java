package com.interviewradar;

import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.repository.StandardizationCandidateRepository;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.service.RawQuestionStandardizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class RawQuestionStandardizationTests {

    @Autowired
    private RawQuestionRepository extractedRepo;

    @Autowired
    private RawQuestionStandardizationService service;

    @Test
    public void testBatchStandardizeExistingData() {
        // 1. 从数据库读取提取问题
        List<RawQuestion> extracted = extractedRepo.findByCandidatesGeneratedFalseAndCategoriesAssignedTrue();

        service.batchStandardize(extracted);
    }
}
