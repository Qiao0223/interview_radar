package com.interviewradar.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.interviewradar.model.enums.CandidateAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDecisionDTO {
    private CandidateAction action;

    // 当 action="REUSE" 时返回
    private Long chosenId;

    // 当 action="CREATE" 时返回
    @JsonProperty("newStandard")
    private String newStandard;

    // 当 action="SKIP" 时返回
    @JsonProperty("skipReason")
    private String skipReason;
}
