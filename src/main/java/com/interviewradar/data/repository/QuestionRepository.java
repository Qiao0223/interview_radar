package com.interviewradar.data.repository;

import com.interviewradar.data.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// InterviewEntity 的 Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

}
