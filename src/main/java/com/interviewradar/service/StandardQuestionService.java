package com.interviewradar.service;

import com.interviewradar.model.dto.StandardQuestionViewDTO;
import com.interviewradar.model.entity.StandardQuestion;
import com.interviewradar.model.entity.StandardizationCandidate;
import com.interviewradar.model.repository.RawToStandardMapRepository;
import com.interviewradar.model.repository.StandardQuestionCategoryRepository;
import com.interviewradar.model.repository.StandardQuestionRepository;
import com.interviewradar.model.repository.StandardizationCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StandardQuestionService {
    private final StandardQuestionRepository questionRepo;
    private final StandardQuestionCategoryRepository sqCatRepo;
    private final RawToStandardMapRepository mapRepo;
    private final StandardizationCandidateRepository candRepo;


    public List<StandardQuestionViewDTO> findAll(String keyword) {
        Sort sort = Sort.by(Sort.Direction.DESC, "usageCount");
        List<StandardQuestion> questions;
        if (keyword != null && !keyword.isBlank()) {
            questions = questionRepo.findByQuestionTextContainingIgnoreCase(keyword, sort);
        } else {
            questions = questionRepo.findAll(sort);
        }
        return questions.stream().map(q -> {
            var categories = sqCatRepo.findByStandardQuestionId(q.getId())
                    .stream().map(c -> c.getCategory().getName()).collect(Collectors.toList());
            var rawQs = mapRepo.findByIdStandardQuestionId(q.getId())
                    .stream().map(m -> m.getRawQuestion().getQuestionText()).collect(Collectors.toList());
            var cands = candRepo.findByMatchedStandardId(q.getId())
                    .stream().map(StandardizationCandidate::getCandidateText).collect(Collectors.toList());
            return StandardQuestionViewDTO.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .status(q.getStatus().name())
                    .usageCount(q.getUsageCount())
                    .updatedAt(q.getUpdatedAt())
                    .categories(categories)
                    .rawQuestions(rawQs)
                    .candidateTexts(cands)
                    .build();
        }).collect(Collectors.toList());
    }

    public Page<StandardQuestionViewDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usageCount"));
        Page<StandardQuestion> result;
        if (keyword != null && !keyword.isBlank()) {
            result = questionRepo.findByQuestionTextContainingIgnoreCase(keyword, pageable);
        } else {
            result = questionRepo.findAll(pageable);
        }
        return result.map(q -> {
            var categories = sqCatRepo.findByStandardQuestionId(q.getId())
                    .stream().map(c -> c.getCategory().getName()).toList();
            var rawQs = mapRepo.findByIdStandardQuestionId(q.getId())
                    .stream().map(m -> m.getRawQuestion().getQuestionText()).toList();
            var cands = candRepo.findByMatchedStandardId(q.getId())
                    .stream().map(c -> c.getCandidateText()).toList();
            return StandardQuestionViewDTO.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .status(q.getStatus().name())
                    .usageCount(q.getUsageCount())
                    .updatedAt(q.getUpdatedAt())
                    .categories(categories)
                    .rawQuestions(rawQs)
                    .candidateTexts(cands)
                    .build();
        });
    }

    public Page<StandardQuestionViewDTO> findAllAggregated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        var projectionPage = questionRepo.findAllAggregated(pageable);

        List<StandardQuestionViewDTO> dtoList = projectionPage.getContent().stream()
                .map(p -> StandardQuestionViewDTO.builder()
                        .id(p.getId())
                        .questionText(p.getQuestionText())
                        .status(p.getStatus())
                        .usageCount(p.getUsageCount())
                        .updatedAt(p.getShowTime())
                        .categories(p.getCategories() != null ? List.of(p.getCategories().split(" \\| ")) : List.of())
                        .rawQuestions(p.getRawQuestions() != null ? List.of(p.getRawQuestions().split(" \\| ")) : List.of())
                        .candidateTexts(p.getCandidateTexts() != null ? List.of(p.getCandidateTexts().split(" \\| ")) : List.of())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, projectionPage.getTotalElements());
    }
}
