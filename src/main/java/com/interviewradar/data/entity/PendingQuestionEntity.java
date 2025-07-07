package com.interviewradar.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pending_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private QuestionEntity question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingStatus status;

    public enum PendingStatus { PENDING, REVIEWED }
}

