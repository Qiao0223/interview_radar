package com.interviewradar.model.repository;

import com.interviewradar.model.entity.StandardQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardQuestionRepository extends JpaRepository<StandardQuestion, Long> {
    List<StandardQuestion> findByQuestionTextContainingIgnoreCase(String keyword, Sort sort);

    Page<StandardQuestion> findByQuestionTextContainingIgnoreCase(String keyword, Pageable pageable);
}
