package com.interviewradar.model.entity;

import com.interviewradar.model.enums.StandardStatus;
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
@Table(name = "standard_question", schema = "interview_radar")
public class StandardQuestion {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

     
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private StandardStatus status;

    @ColumnDefault("1")
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "creator", nullable = false, length = 50)
    private String creator;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewer", length = 50)
    private String reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

     
    @Column(name = "review_notes")
    private String reviewNotes;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "embedding", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> embedding;

}