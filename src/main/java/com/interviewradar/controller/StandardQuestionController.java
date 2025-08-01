package com.interviewradar.controller;

import com.interviewradar.model.dto.StandardQuestionViewDTO;
import com.interviewradar.service.StandardQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class StandardQuestionController {

    private final StandardQuestionService service;

    @GetMapping("/standard-questions")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Page<?> result = service.findAllAggregated(page, size);
        model.addAttribute("page", result);
        return "standard_questions";
    }
}
