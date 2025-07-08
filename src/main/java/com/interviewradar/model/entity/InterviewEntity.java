package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewEntity {
    @Id
    @Column(name = "content_id")
    private Long contentId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "show_time")
    private LocalDateTime showTime;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "extracted", nullable = false)
    private boolean questionsExtracted;
}

