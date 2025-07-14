package com.interviewradar.model.repository;

import com.interviewradar.model.entity.StandardizationCandidate;
import com.interviewradar.model.enums.CandidateDecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardizationCandidateRepository extends JpaRepository<StandardizationCandidate, Long> {


    List<StandardizationCandidate> findByDecisionStatus(CandidateDecisionStatus decisionStatus);

    Optional<StandardizationCandidate> findFirstByDecisionStatusOrderByCreatedAtAsc(CandidateDecisionStatus decisionStatus);
}
