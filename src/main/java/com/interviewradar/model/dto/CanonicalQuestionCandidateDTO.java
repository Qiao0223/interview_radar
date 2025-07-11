package com.interviewradar.model.dto;

import com.interviewradar.model.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a retrieved canonical question candidate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalQuestionCandidateDTO {
    private Long id;
    private String text;
    private ReviewStatus status;
    private float score;
}