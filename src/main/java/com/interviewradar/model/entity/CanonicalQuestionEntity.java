package com.interviewradar.model.entity;

import com.interviewradar.model.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "canonical_question", uniqueConstraints = @UniqueConstraint(columnNames = "text"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    // ✅ 向量字段（建议用 JSON 字符串存储）
    @Column(name = "embedding", columnDefinition = "JSON")
    private String embedding;

    @Column(name = "count", nullable = false)
    private Integer count = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ReviewStatus status;  // PENDING, APPROVED, REJECTED

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    // ✅ 多对多：canonical_question ↔ category（替代原来单一 category）
    @ManyToMany
    @JoinTable(
            name = "canonical_question_category",
            joinColumns = @JoinColumn(name = "canonical_question_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryEntity> categories;

    // 多对多：extracted_question ↔ canonical_question
    @ManyToMany(mappedBy = "canonicalQuestions")
    private Set<ExtractedQuestionEntity> extractedQuestions;

    // 多对多：canonical_question ↔ topic
    @ManyToMany
    @JoinTable(
            name = "canonical_question_topic",
            joinColumns = @JoinColumn(name = "canonical_question_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<TopicEntity> topics;
}
