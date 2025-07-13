package com.interviewradar.model.entity;

import com.interviewradar.model.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "standardization_candidate", schema = "interview_radar", indexes = {
        @Index(name = "source_question_id", columnList = "raw_question_id"),
        @Index(name = "matched_canonical_id", columnList = "matched_standard_id")
})
public class StandardizationCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

     
    @Column(name = "candidate_text", nullable = false)
    private String candidateText;

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.JSON)
    private String embedding;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_question_id", nullable = false)
    private RawQuestion rawQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_standard_id")
    private StandardQuestion matchedStandard;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private CandidateStatus status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime generatedAt;

}