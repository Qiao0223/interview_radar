package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "extracted_question")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExtractedQuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "classified", nullable = false)
    private boolean classified = false;

    @Column(name = "parsed", nullable = false)
    private boolean parsed;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;


    // 多对多：问题 ↔ 分类
    @ManyToMany
    @JoinTable(
            name="question_to_category",
            joinColumns=@JoinColumn(name="question_id"),
            inverseJoinColumns=@JoinColumn(name="category_id")
    )
    private Set<CategoryEntity> categories;

    @ManyToMany
    @JoinTable(
            name="extracted_question_canonical",
            joinColumns=@JoinColumn(name="extracted_question_id"),
            inverseJoinColumns=@JoinColumn(name="canonical_question_id")
    )
    private Set<CanonicalQuestionEntity> canonicalQuestions;


}

