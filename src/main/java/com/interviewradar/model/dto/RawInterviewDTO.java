package com.interviewradar.model.dto; // 或者 com.interviewradar.data，看你实际放哪

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始面经 DTO，仅用来承载从列表接口抓下来的字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawInterviewDTO {
    private Long contentId;
    private String title;
    private String content;
    private LocalDateTime showTime;
}

