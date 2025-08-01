package com.interviewradar.model.repository;

import com.interviewradar.model.entity.StandardQuestion;
import com.interviewradar.model.projection.StandardQuestionViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StandardQuestionRepository extends JpaRepository<StandardQuestion, Long> {
    List<StandardQuestion> findByQuestionTextContainingIgnoreCase(String keyword, Sort sort);

    Page<StandardQuestion> findByQuestionTextContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(
            value = "SELECT " +
                    "sq.id AS id, " +
                    "sq.question_text AS questionText, " +
                    "sq.status AS status, " +
                    "sq.usage_count AS usageCount, " +
                    "MAX(ri.show_time) AS showTime, " +
                    "GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name SEPARATOR ' | ') AS categories, " +
                    "GROUP_CONCAT(DISTINCT rq.question_text ORDER BY rq.id SEPARATOR ' | ') AS rawQuestions, " +
                    "GROUP_CONCAT(DISTINCT cand.candidate_text ORDER BY cand.id SEPARATOR ' | ') AS candidateTexts " +
                    "FROM standard_question sq " +
                    "LEFT JOIN standard_question_category sqc ON sq.id = sqc.standard_question_id " +
                    "LEFT JOIN category cat ON sqc.category_id = cat.id " +
                    "LEFT JOIN raw_to_standard_map r2s ON sq.id = r2s.standard_question_id " +
                    "LEFT JOIN raw_question rq ON r2s.raw_question_id = rq.id " +
                    "LEFT JOIN raw_interview ri ON rq.interview_id = ri.content_id " +
                    "LEFT JOIN standardization_candidate cand ON cand.matched_standard_id = sq.id " +
                    "GROUP BY sq.id, sq.question_text, sq.status, sq.usage_count " +
                    "ORDER BY sq.usage_count DESC",
            countQuery = "SELECT COUNT(*) FROM standard_question",
            nativeQuery = true
    )
    Page<StandardQuestionViewProjection> findAllAggregated(Pageable pageable);
}
