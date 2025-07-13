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
@Table(name = "raw_to_standard_map", schema = "interview_radar", indexes = {
        @Index(name = "idx_eqc_extracted", columnList = "raw_question_id"),
        @Index(name = "idx_eqc_canonical", columnList = "standard_question_id")
})
public class RawToStandardMap {
    @EmbeddedId
    private RawToStandardMapId id;

    @MapsId("rawQuestionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "raw_question_id", nullable = false)
    private RawQuestion rawQuestion;

    @MapsId("standardQuestionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "standard_question_id", nullable = false)
    private StandardQuestion standardQuestion;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "mapped_at", nullable = false)
    private LocalDateTime mappedAt;

}