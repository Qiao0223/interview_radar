package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawInterview;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawInterviewRepository extends JpaRepository<RawInterview, Long> {
    boolean existsById(@NotNull Long contentId);

    List<RawInterview> findByQuestionsExtractedFalse();
}

