package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawQuestionCategory;
import com.interviewradar.model.entity.RawQuestionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawQuestionCategoryRepository extends JpaRepository<RawQuestionCategory, RawQuestionCategoryId> {
}