package com.interviewradar.model.repository;

import com.interviewradar.model.entity.RawQuestion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// InterviewEntity 的 Repository
public interface RawQuestionRepository extends JpaRepository<RawQuestion, Long> {

    List<RawQuestion> findByCandidatesGeneratedFalseAndCategoriesAssignedTrue();

    List<RawQuestion> findByCategoriesAssignedFalse();

    List<RawQuestion> findTop1ByCategoriesAssignedFalse();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rq from RawQuestion rq where rq.id = :id")
    RawQuestion findByIdForUpdate(@Param("id") Long id);

}
