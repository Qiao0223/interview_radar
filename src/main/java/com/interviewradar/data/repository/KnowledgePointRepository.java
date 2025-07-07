package com.interviewradar.data.repository;

import com.interviewradar.data.entity.KnowledgePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// InterviewEntity 的 Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePointEntity, Long> {

}
