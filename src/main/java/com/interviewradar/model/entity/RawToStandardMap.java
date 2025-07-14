package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Builder
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

    public RawToStandardMap(RawToStandardMapId id,
                            RawQuestion rawQuestion,
                            StandardQuestion standardQuestion,
                            LocalDateTime mappedAt) {
        this.id = id;
        this.rawQuestion = rawQuestion;
        this.standardQuestion = standardQuestion;
        this.mappedAt = (mappedAt != null) ? mappedAt : LocalDateTime.now();
    }

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

    /**
     * 在保存前自动填充 mappedAt
     */
    @PrePersist
    private void prePersist() {
        if (mappedAt == null) {
            mappedAt = LocalDateTime.now();
        }
    }

}