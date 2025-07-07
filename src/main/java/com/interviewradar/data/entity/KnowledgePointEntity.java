package com.interviewradar.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knowledge_points",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id","name"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgePointEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(nullable = false, length = 128)
    private String name;
}
