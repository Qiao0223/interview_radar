package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawToStandardMap;
import com.interviewradar.model.entity.RawToStandardMapId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawToStandardMapRepository extends JpaRepository<RawToStandardMap, RawToStandardMapId> {
}