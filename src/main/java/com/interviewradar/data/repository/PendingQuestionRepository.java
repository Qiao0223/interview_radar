package com.interviewradar.data.repository;

import com.interviewradar.data.entity.PendingQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingQuestionRepository extends JpaRepository<PendingQuestionEntity, Long> {

}
