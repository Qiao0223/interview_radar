package com.interviewradar.model.repository;

import com.interviewradar.model.entity.CanonicalQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanonicalQuestionRepository extends JpaRepository<CanonicalQuestionEntity, Long> {
}
