package com.interviewradar.model.repository;

import com.interviewradar.model.entity.PendingQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingQuestionRepository extends JpaRepository<PendingQuestionEntity, Long> {

}
