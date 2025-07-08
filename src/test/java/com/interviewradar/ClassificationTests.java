package com.interviewradar;

import com.interviewradar.model.entity.CategoryEntity;
import com.interviewradar.model.entity.ExtractedQuestionEntity;
import com.interviewradar.model.entity.InterviewEntity;
import com.interviewradar.model.repository.CategoryRepository;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.model.repository.InterviewRepository;
import dev.langchain4j.model.chat.ChatModel;
import com.interviewradar.service.ClassificationService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClassificationTests {

    @Autowired
    ClassificationService classificationService;
    @Autowired
    ChatModel chatModel;
    @Autowired
    InterviewRepository interviewRepo;
    @Autowired
    CategoryRepository categoryRepo;
    @Autowired
    ExtractedQuestionRepository questionRepo;

    private InterviewEntity testInterview;
    private CategoryEntity catA;
    private CategoryEntity catB;


    @Test
    public void classifyBatch_directDbAndRealLlm() {
        // 从数据库读取所有 Question，并调用 classifyBatch
        List<ExtractedQuestionEntity> questions = questionRepo.findAll();
        classificationService.classifyBatch(questions);

        // 重新从数据库加载，验证每条题目都被标记为已分类，且至少有一个 Category
        List<ExtractedQuestionEntity> updated = questionRepo.findAll();

        for (ExtractedQuestionEntity q : updated) {
            Assertions.assertTrue(q.isClassified(),
                    "题目 '" + q.getQuestionText() + "' 应被标记为已分类");
            Assertions.assertFalse(q.getCategories().isEmpty(),
                    "题目 '" + q.getQuestionText() + "' 应至少有一个分类");
        }
    }
}
