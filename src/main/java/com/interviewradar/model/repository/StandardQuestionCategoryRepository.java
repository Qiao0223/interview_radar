package com.interviewradar.model.repository;

import com.interviewradar.model.entity.StandardQuestionCategory;
import com.interviewradar.model.entity.StandardQuestionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardQuestionCategoryRepository extends JpaRepository<StandardQuestionCategory, StandardQuestionCategoryId> {
    List<StandardQuestionCategory> findByStandardQuestionId(Long standardQuestionId);
}