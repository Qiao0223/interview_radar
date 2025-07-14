package com.interviewradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewradar.model.entity.RawInterview;
import com.interviewradar.model.repository.RawInterviewRepository;
import com.interviewradar.model.dto.RawInterviewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.LocalDateTime;
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
    private RawInterviewRepository interviewRepo;

    /**
     * 抓取指定页的原始面经 DTO 列表
     */
    public List<RawInterviewDTO> fetchListPage(int page) {
        long ts = System.currentTimeMillis();
        JsonNode root = restTemplate.getForObject(LIST_API, JsonNode.class, page, ts);
        assert root != null;
        JsonNode records = root.path("data").path("records");

        if (!records.isArray() || records.isEmpty()) {
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
        return Instant.ofEpochMilli(ms)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime();
    }

    private LocalDateTime toLocalDateTimeOrNull(long ms) {
        return ms <= 0 ? null : toLocalDateTime(ms);
    }

    /**
     * 增量爬取新面经：从第 1 页开始，遇到已存在 contentId 即停止
     */
    public void crawlNewInterviews() {
        int page = 1;
        outer:
        while (true) {
            List<RawInterviewDTO> list;
            try {
                // 获取指定页的新数据列表
                list = fetchListPage(page);
            } catch (Exception e) {
                log.error("抓取第 {} 页面经列表时发生异常，停止爬取", page, e);
                break; // 发生错误时终止整个爬取
            }

            if (list.isEmpty()) {
                log.info("第 {} 页无新数据，停止增量爬取", page);
                break;
            }

            for (RawInterviewDTO dto : list) {
                try {
                    // 如果已存在，则停止增量爬取
                    if (interviewRepo.existsById(dto.getContentId())) {
                        log.info("遇到已存在的面经 contentId={}，停止增量爬取", dto.getContentId());
                        break outer;
                    }
                    // 保存新面经
                    saveInterview(dto);
                } catch (Exception e) {
                    log.error("处理面经 contentId={} 时发生异常，跳过此条记录", dto.getContentId(), e);
                    // 遇到单条记录异常，跳过继续下一条
                    continue;
                }
            }

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                log.warn("线程休眠被中断，停止爬取", ie);
                Thread.currentThread().interrupt(); // 恢复中断状态
                break;
            }

            page++;
        }
        log.info("增量爬取完成");
    }


    /**
     * 去重并保存一条面经到 interviews 表
     */
    private void saveInterview(RawInterviewDTO dto) {
        if (interviewRepo.existsById(dto.getContentId())) {
            log.debug("已存在，跳过 id={}", dto.getContentId());
            return;
        }
        RawInterview iv = RawInterview.builder()
                .id(dto.getContentId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .showTime(LocalDateTime.from(dto.getShowTime()))
                .fetchedAt( LocalDateTime.now())
                .questionsExtracted(false)
                .build();
        interviewRepo.save(iv);
        log.info("已保存面经 contentId={}", dto.getContentId());
    }
}
