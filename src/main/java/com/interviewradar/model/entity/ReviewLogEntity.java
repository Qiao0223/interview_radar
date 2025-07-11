package com.interviewradar.model.entity;

import com.interviewradar.model.enums.ReviewAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="entity_review_log", indexes=@Index(name="idx_review", columnList="entity_type,entity_id,action"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLogEntity {
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name="entity_type", nullable=false, length=20)
    private String entityType; // "TOPIC" or "CANONICAL"

    @Column(name="entity_id", nullable=false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name="action", nullable=false, length=10)
    private ReviewAction action; // APPROVE, REJECT

    @Column(name="reviewer", nullable=false, length=100)
    private String reviewer;

    @Column(name="review_comment", columnDefinition="TEXT")
    private String reviewComment;

    @Column(name="action_time", nullable=false)
    private LocalDateTime actionTime;
}
