package com.interviewradar.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "raw_question", schema = "interview_radar", indexes = {
        @Index(name = "idx_q_interview", columnList = "interview_id")
})
public class RawQuestion {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "interview_id", nullable = false)
    private RawInterview interview;

     
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ColumnDefault("0")
    @Column(name = "candidates_generated", nullable = false)
    private Boolean candidatesGenerated = false;

    @ColumnDefault("0")
    @Column(name = "categories_assigned", nullable = false)
    private Boolean categoriesAssigned = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rawQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RawQuestionCategory> rawQuestionCategories = new HashSet<>();

    public Set<Category> getCategories() {
        return rawQuestionCategories.stream()
                .map(RawQuestionCategory::getCategory)
                .collect(Collectors.toSet());
    }

}