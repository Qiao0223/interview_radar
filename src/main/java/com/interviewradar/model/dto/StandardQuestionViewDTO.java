package com.interviewradar.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardQuestionViewDTO {
    private Long id;
    private String questionText;
    private String status;
    private Integer usageCount;
    private LocalDateTime updatedAt;
    private List<String> categories;
    private List<String> candidateTexts;
    private List<String> rawQuestions;
}
