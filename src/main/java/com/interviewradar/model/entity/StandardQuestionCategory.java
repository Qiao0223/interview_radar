package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "standard_question_category", schema = "interview_radar", indexes = {
        @Index(name = "category_id", columnList = "category_id")
})
public class StandardQuestionCategory {
    @EmbeddedId
    private StandardQuestionCategoryId id;

    @MapsId("standardQuestionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standard_question_id", nullable = false)
    private StandardQuestion standardQuestion;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

}