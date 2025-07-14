package com.interviewradar.model.repository;

import com.interviewradar.model.entity.PromptLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptLogRepository extends JpaRepository<PromptLog, Long> {
}