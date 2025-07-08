package com.interviewradar;

import com.interviewradar.crawler.CrawlerService;
import com.interviewradar.model.repository.InterviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CrawlerTests {

    @Autowired
    private CrawlerService crawler;

    @Autowired
    private InterviewRepository interviewRepo;


    @Test
    void testCrawlNewInterviews() throws InterruptedException {
        // 调用增量爬取
        crawler.crawlNewInterviews();
        // 爬完后，库里应该有 N (>0) 条数据
        long firstCount = interviewRepo.count();
        System.out.println("首次爬取条数: " + firstCount);
        assertTrue(firstCount > 0, "应该至少爬到一条面经");

        // 再跑一次，因遇到重复就会停，数据量不应再增加
        crawler.crawlNewInterviews();
        long secondCount = interviewRepo.count();
        System.out.println("二次爬取后条数: " + secondCount);
        assertEquals(firstCount, secondCount, "二次爬取不应新增数据");
    }
}
