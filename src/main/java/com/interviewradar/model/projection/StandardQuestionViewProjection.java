package com.interviewradar.model.projection;

import java.time.LocalDateTime;

public interface StandardQuestionViewProjection {
    Long getId();
    String getQuestionText();
    String getStatus();
    Integer getUsageCount();
    LocalDateTime getShowTime();
    String getCategories();
    String getRawQuestions();
    String getCandidateTexts();
}
