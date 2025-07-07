package com.interviewradar;

import com.interviewradar.data.entity.CategoryEntity;
import com.interviewradar.data.entity.InterviewEntity;
import com.interviewradar.data.entity.QuestionEntity;
import com.interviewradar.data.repository.CategoryRepository;
import com.interviewradar.data.repository.InterviewRepository;
import com.interviewradar.data.repository.QuestionRepository;
import com.interviewradar.llm.LanguageModel;
import com.interviewradar.service.ClassificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "llm.provider=aliyun")
public class ClassificationTests {

    @Autowired
    ClassificationService classificationService;
    @Autowired
    LanguageModel llm;
    @Autowired
    InterviewRepository interviewRepo;
    @Autowired
    CategoryRepository categoryRepo;
    @Autowired
    QuestionRepository questionRepo;

    private InterviewEntity testInterview;
    private CategoryEntity catA;
    private CategoryEntity catB;

    @Before  // JUnit4 的生命周期注解
    @Transactional
    public void setUp() {
    }

    @Commit
    @Test
    @Transactional
    public void classifyBatch_directDbAndRealLlm() {
        // 从数据库读取所有 Question，并调用 classifyBatch
        List<QuestionEntity> questions = questionRepo.findAll();
        classificationService.classifyBatch(questions);

        // 重新从数据库加载，验证每条题目都被标记为已分类，且至少有一个 Category
        List<QuestionEntity> updated = questionRepo.findAll();

        for (QuestionEntity q : updated) {
            Assertions.assertTrue(q.isClassified(),
                    "题目 '" + q.getQuestionText() + "' 应被标记为已分类");
            Assertions.assertFalse(q.getCategories().isEmpty(),
                    "题目 '" + q.getQuestionText() + "' 应至少有一个分类");
        }
    }
}
