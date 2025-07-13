package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "raw_question_category", schema = "interview_radar", indexes = {
        @Index(name = "idx_q2c_question", columnList = "raw_question_id"),
        @Index(name = "idx_q2c_category", columnList = "category_id")
})
public class RawQuestionCategory {
    @EmbeddedId
    private RawQuestionCategoryId id;

    @MapsId("rawQuestionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "raw_question_id", nullable = false)
    private RawQuestion rawQuestion;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

}