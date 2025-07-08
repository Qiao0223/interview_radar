package com.interviewradar.model.repository;

import com.interviewradar.model.entity.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {
    boolean existsByContentId(Long contentId);
}

