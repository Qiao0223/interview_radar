package com.interviewradar.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "questions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionEntity {
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

    // 多对多：问题 ↔ 分类
    @ManyToMany
    @JoinTable(
            name = "question_categories",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryEntity> categories;

    // 多对多：问题 ↔ 知识点
    @ManyToMany
    @JoinTable(
            name = "question_knowledge_points",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "knowledge_point_id")
    )
    private Set<KnowledgePointEntity> knowledgePoints;
}

