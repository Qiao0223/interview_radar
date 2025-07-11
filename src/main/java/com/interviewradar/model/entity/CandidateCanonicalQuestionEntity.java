package com.interviewradar.model.entity;

import com.interviewradar.model.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_canonical_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateCanonicalQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "embedding", columnDefinition = "JSON")
    private String embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_question_id", nullable = false)
    private ExtractedQuestionEntity sourceQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_canonical_id")
    private CanonicalQuestionEntity matchedCanonical;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private CandidateStatus status = CandidateStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
