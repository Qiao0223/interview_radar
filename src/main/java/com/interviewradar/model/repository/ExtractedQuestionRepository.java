package com.interviewradar.model.repository;

import com.interviewradar.model.entity.ExtractedQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// InterviewEntity 的 Repository
public interface ExtractedQuestionRepository extends JpaRepository<ExtractedQuestionEntity, Long> {

}
