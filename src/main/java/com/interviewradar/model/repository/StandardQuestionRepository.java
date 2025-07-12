package com.interviewradar.model.repository;

import com.interviewradar.model.entity.StandardQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardQuestionRepository extends JpaRepository<StandardQuestion, Long> {
}
