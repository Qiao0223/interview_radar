package com.interviewradar.model.entity;

import com.interviewradar.model.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id","name"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TopicEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name="description", length=2048)
    private String description;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="occurrence_count", nullable=false)
    private Integer occurrenceCount = 0;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Column(name="created_by", nullable=false, length=50)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=10)
    private ReviewStatus status;

    @Column(name="reviewed_by", length=50)
    private String reviewedBy;

    @Column(name="reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name="review_comment", columnDefinition="TEXT")
    private String reviewComment;

}
