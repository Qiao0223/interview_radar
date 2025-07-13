package com.interviewradar.model.entity;

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
@Table(name = "raw_interview", schema = "interview_radar")
public class RawInterview {
    @Id
    @Column(name = "content_id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

     
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "show_time")
    private LocalDateTime showTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @ColumnDefault("0")
    @Column(name = "questions_extracted", nullable = false)
    private Boolean questionsExtracted = false;

}