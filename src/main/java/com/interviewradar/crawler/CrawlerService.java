package com.interviewradar.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewradar.data.entity.InterviewEntity;
import com.interviewradar.data.repository.InterviewRepository;
import com.interviewradar.data.dto.RawInterviewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    /**
     * 列表 API，{page} 和 {ts} 会被替换
     */
    private static final String LIST_API =
            "https://gw-c.nowcoder.com/api/sparta/home/tab/content"
                    + "?pageNo={page}&categoryType=1&tabId=818&_={ts}";

    @Value("${crawler.delay-ms}")
    private long delayMs;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InterviewRepository interviewRepo;

    /**
     * 抓取指定页的原始面经 DTO 列表
     */
    public List<RawInterviewDTO> fetchListPage(int page) {
        long ts = System.currentTimeMillis();
        JsonNode root = restTemplate.getForObject(LIST_API, JsonNode.class, page, ts);
        JsonNode records = root.path("data").path("records");

        if (!records.isArray() || records.size() == 0) {
            return Collections.emptyList();
        }

        List<RawInterviewDTO> list = new ArrayList<>();
        for (JsonNode item : records) {
            // 1. 全局唯一 ID
            long contentId = item.path("contentId").asLong();

            // 2. 根据 contentType 选择不同的子节点
            int contentType = item.path("contentType").asInt();
            JsonNode bodyNode;
            if (contentType == 74) {
                // 面经类型：要读 momentData
                bodyNode = item.path("momentData");
            } else if (contentType == 250) {
                // 讨论帖类型：要读 contentData
                bodyNode = item.path("contentData");
            } else {
                // 其它类型先跳过
                continue;
            }

            // 3. 标题、正文
            String title   = bodyNode.path("title").asText("");
            String content = bodyNode.path("content").asText("");

            // 4. 时间字段也在 bodyNode 下
            long showMs    = bodyNode.path("showTime").asLong(0);

            // 5. 构造 DTO
            RawInterviewDTO dto = RawInterviewDTO.builder()
                    .contentId(contentId)
                    .title(title)
                    .content(content)
                    .showTime(toLocalDateTimeOrNull(showMs))
                    .build();

            list.add(dto);
        }
        return list;
    }

    // 工具方法，把毫秒转成 LocalDateTime
    private LocalDateTime toLocalDateTime(long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
    }

    private LocalDateTime toLocalDateTimeOrNull(long ms) {
        return ms <= 0 ? null : toLocalDateTime(ms);
    }

    /**
     * 增量爬取新面经：从第 1 页开始，遇到已存在 contentId 即停止
     */
    public void crawlNewInterviews() throws InterruptedException {
        int page = 1;
        outer:
        while (true) {
            List<RawInterviewDTO> list = fetchListPage(page);
            if (list.isEmpty()) {
                log.info("第 {} 页无新数据，停止增量爬取", page);
                break;
            }
            for (RawInterviewDTO dto : list) {
                if (interviewRepo.existsByContentId(dto.getContentId())) {
                    log.info("遇到已存在的面经 contentId={}，停止增量爬取", dto.getContentId());
                    break outer;
                }
                saveInterview(dto);
            }
            Thread.sleep(delayMs);
            page++;
        }
        log.info("增量爬取完成");
    }

    /**
     * 去重并保存一条面经到 interviews 表
     */
    private void saveInterview(RawInterviewDTO dto) {
        if (interviewRepo.existsByContentId(dto.getContentId())) {
            log.debug("已存在，跳过 contentId={}", dto.getContentId());
            return;
        }
        InterviewEntity iv = InterviewEntity.builder()
                .contentId(dto.getContentId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .showTime(dto.getShowTime())
                .fetchedAt(LocalDateTime.now())
                .build();
        interviewRepo.save(iv);
        log.info("已保存面经 contentId={}", dto.getContentId());
    }
}
