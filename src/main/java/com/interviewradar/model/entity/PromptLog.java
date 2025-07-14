package com.interviewradar.model.entity;

import com.interviewradar.model.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "prompt_log", schema = "interview_radar")
public class PromptLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "model_name", length = 100)
    private String modelName;


    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "response")
    private String response;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @ColumnDefault("1")
    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}