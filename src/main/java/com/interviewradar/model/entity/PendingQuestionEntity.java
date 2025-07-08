package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="reviewed_by", length=50)
    private String reviewedBy;

    @Column(name="reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name="review_comment", columnDefinition="TEXT")
    private String reviewComment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private ExtractedQuestionEntity question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingStatus status;

    public enum PendingStatus { PENDING, REVIEWED }
}

