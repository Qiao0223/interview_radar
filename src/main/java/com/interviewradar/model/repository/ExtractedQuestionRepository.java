package com.interviewradar.model.repository;

import com.interviewradar.model.entity.ExtractedQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// InterviewEntity 的 Repository
public interface ExtractedQuestionRepository extends JpaRepository<ExtractedQuestionEntity, Long> {

    List<ExtractedQuestionEntity> findByCanonicalizedFalseAndCategorizedTrue();

    List<ExtractedQuestionEntity> findByCategorizedFalse();
}
