package com.interviewradar.data.repository;

import com.interviewradar.data.entity.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {
    boolean existsByContentId(Long contentId);
}

