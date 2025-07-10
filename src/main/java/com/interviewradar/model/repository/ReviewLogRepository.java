package com.interviewradar.model.repository;

import com.interviewradar.model.entity.ReviewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {
}
