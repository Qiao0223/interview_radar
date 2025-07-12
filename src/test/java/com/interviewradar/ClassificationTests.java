package com.interviewradar;

import com.interviewradar.model.entity.Category;
import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.entity.RawQuestion;
import com.interviewradar.model.repository.CategoryRepository;
import com.interviewradar.model.repository.RawQuestionRepository;
import com.interviewradar.model.repository.RawInterviewRepository;
import dev.langchain4j.model.chat.ChatModel;
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
    ChatModel chatModel;
    @Autowired
    RawInterviewRepository interviewRepo;
    @Autowired
    CategoryRepository categoryRepo;
    @Autowired
    RawQuestionRepository questionRepo;

    private RawInterview testInterview;
    private Category catA;
    private Category catB;


    @Test
    public void classifyBatch_directDbAndRealLlm() {
        // 从数据库读取所有 Question，并调用 classifyBatch
        List<RawQuestion> questions = questionRepo.findByCategoriesAssignedFalse();
        classificationService.classifyBatch(questions);

        // 重新从数据库加载，验证每条题目都被标记为已分类，且至少有一个 Category
        List<RawQuestion> updated = questionRepo.findAll();

    }
}
