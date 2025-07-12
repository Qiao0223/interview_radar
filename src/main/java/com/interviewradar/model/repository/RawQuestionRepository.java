package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// InterviewEntity 的 Repository
public interface RawQuestionRepository extends JpaRepository<RawQuestion, Long> {

    List<RawQuestion> findByCandidatesGeneratedFalseAndCategoriesAssignedTrue();

    List<RawQuestion> findByCategoriesAssignedFalse();
}
