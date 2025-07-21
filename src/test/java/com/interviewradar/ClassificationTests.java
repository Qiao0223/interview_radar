package com.interviewradar;

import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.service.RawQuestionClassificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClassificationTests {

    @Autowired
    RawQuestionClassificationService classificationService;

    @Autowired
    RawQuestionRepository questionRepo;

    @Test
    public void classifyBatchSingle() {
        List<RawQuestion> questions = questionRepo.findTop1ByCategoriesAssignedFalse();
        classificationService.classifyBatch(questions);
    }

    @Test
    public void classifyBatchAll() {
        List<RawQuestion> questions = questionRepo.findByCategoriesAssignedFalse();
        classificationService.classifyBatch(questions);
    }
}
