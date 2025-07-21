package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawToStandardMap;
import com.interviewradar.model.entity.RawToStandardMapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawToStandardMapRepository extends JpaRepository<RawToStandardMap, RawToStandardMapId> {
    List<RawToStandardMap> findByIdStandardQuestionId(Long standardQuestionId);
}