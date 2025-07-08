package com.interviewradar;

import com.interviewradar.model.entity.InterviewEntity;
import com.interviewradar.model.repository.ExtractedQuestionRepository;
import com.interviewradar.model.repository.InterviewRepository;
import com.interviewradar.llm.LanguageModel;
import com.interviewradar.service.QuestionExtractionService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "llm.provider=aliyun")
public class QuestionExtractionTests {

    @Autowired
    QuestionExtractionService service;
    @Autowired
    LanguageModel llm;
    @Autowired
    InterviewRepository interviewRepo;
    @Autowired
    ExtractedQuestionRepository qRepo;

    @Test
    public void llmGenerateCanBeCalled() throws Exception {
        // 验证 llm.generate 方法本身能正常调用
        String out = llm.generate("hello");
        Assertions.assertNotNull(out); // 可选，验证返回结果非空
    }

    @Test
    public void extractQuestionsTriggersLlmGenerate() throws Exception {
        // 验证 service.extractQuestions 能顺利调用 llm.generate 并不抛异常
        List<String> qs = service.extractQuestions(
                "快离职啦，面经发出来给大家\n" +
                        "一面 （25/3/10）\n" +
                        "    答的不是很好，但是面试官非常好，跟我说了很多东西，而且非常有耐心，感恩。\n" +
                        "    实习能够保证6个月，每周至少4天吗？\n" +
                        "    1、自我介绍\n" +
                        "    2、synchronized的底层原理？\n" +
                        "    3、字节码层面上相关的指令有了解吗？\n" +
                        "    4、synchronized锁升级和优化。\n" +
                        "    5、偏向锁是怎么实现的？轻量级锁、重量级锁在操作系统层面怎么实现的，有了解过吗？\n" +
                        "    6、介绍一下volatile的实现原理，说一说JMM。\n" +
                        "    7、还有一个作用。（防止指令重排序）\n" +
                        "    8、从操作系统的层面取理解Java的线程有哪些部分？或者有哪些组成元素？\n" +
                        "    9、线程进行上下文切换的时候都需要哪些东西来保证线程能够恢复到原来的待运行状态？\n"
        );
        Assertions.assertNotNull(qs); // 可选，验证返回值不为 null
        Assertions.assertFalse(qs.isEmpty()); // 可选，验证确实抽出了一些问题
    }

    @Test
    public void extractAndSaveQuestions() throws Exception {
        InterviewEntity interview = interviewRepo.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("数据库中无 InterviewEntity 数据"));
        service.extractAndSave(interview);
    }

}
