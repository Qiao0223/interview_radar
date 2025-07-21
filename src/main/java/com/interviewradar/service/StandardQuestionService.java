package com.interviewradar.service;

import com.interviewradar.model.dto.StandardQuestionViewDTO;
import com.interviewradar.model.entity.StandardQuestion;
import com.interviewradar.model.repository.RawToStandardMapRepository;
import com.interviewradar.model.repository.StandardQuestionCategoryRepository;
import com.interviewradar.model.repository.StandardQuestionRepository;
import com.interviewradar.model.repository.StandardizationCandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StandardQuestionService {
    private final StandardQuestionRepository questionRepo;
    private final StandardQuestionCategoryRepository sqCatRepo;
    private final RawToStandardMapRepository mapRepo;
    private final StandardizationCandidateRepository candRepo;

    @Autowired
    public StandardQuestionService(StandardQuestionRepository questionRepo,
                                   StandardQuestionCategoryRepository sqCatRepo,
                                   RawToStandardMapRepository mapRepo,
                                   StandardizationCandidateRepository candRepo) {
        this.questionRepo = questionRepo;
        this.sqCatRepo = sqCatRepo;
        this.mapRepo = mapRepo;
        this.candRepo = candRepo;
    }

    public List<StandardQuestionViewDTO> findAll() {
        List<StandardQuestion> questions = questionRepo.findAll(Sort.by(Sort.Direction.DESC, "usageCount"));
        return questions.stream().map(q -> {
            List<String> categories = sqCatRepo.findByStandardQuestionId(q.getId())
                    .stream()
                    .map(c -> c.getCategory().getName())
                    .collect(Collectors.toList());
            List<String> rawQuestions = mapRepo.findByIdStandardQuestionId(q.getId())
                    .stream()
                    .map(m -> m.getRawQuestion().getQuestionText())
                    .collect(Collectors.toList());
            List<String> candidates = candRepo.findByMatchedStandardId(q.getId())
                    .stream()
                    .map(c -> c.getCandidateText())
                    .collect(Collectors.toList());
            return StandardQuestionViewDTO.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .status(q.getStatus().name())
                    .usageCount(q.getUsageCount())
                    .updatedAt(q.getUpdatedAt())
                    .categories(categories)
                    .candidateTexts(candidates)
                    .rawQuestions(rawQuestions)
                    .build();
        }).collect(Collectors.toList());
    }
}
