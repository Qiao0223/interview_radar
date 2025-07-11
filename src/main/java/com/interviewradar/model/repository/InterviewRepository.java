package com.interviewradar.model.repository;

import com.interviewradar.model.entity.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {
    boolean existsByContentId(Long contentId);

    List<InterviewEntity> findByQuestionsExtractedFalse();
}

