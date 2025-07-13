package com.interviewradar.model.entity;

import com.interviewradar.model.enums.ReviewAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "entity_review_log", schema = "interview_radar", indexes = {
        @Index(name = "idx_erl_entity", columnList = "entity_type, entity_id")
})
public class EntityReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "entity_type", length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ReviewAction action;

    @Column(name = "reviewer", nullable = false, length = 100)
    private String reviewer;

    @Column(name = "review_comment")
    private String reviewComment;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

}