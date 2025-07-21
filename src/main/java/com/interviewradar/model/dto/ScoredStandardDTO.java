package com.interviewradar.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScoredStandardDTO {
    private Long id;
    private Float score;
    private String status;
    private String questionText;
}
