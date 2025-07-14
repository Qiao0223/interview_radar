package com.interviewradar;

import com.interviewradar.model.entity.StandardizationCandidate;
import com.interviewradar.model.enums.CandidateDecisionStatus;
import com.interviewradar.model.repository.StandardizationCandidateRepository;
import com.interviewradar.service.StandardizationJudgementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class StandardizationJudgementTests {

    @Autowired
    StandardizationJudgementService judgementService;

    @Autowired
    private StandardizationCandidateRepository candidateRepo;

    @Test
    public void standardizationJudgementTest() {
        StandardizationCandidate cand = candidateRepo
                .findFirstByDecisionStatusOrderByCreatedAtAsc(CandidateDecisionStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No candidate found"));

        judgementService.processSingle(cand);
    }
}
