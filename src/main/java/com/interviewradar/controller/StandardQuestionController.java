package com.interviewradar.controller;

import com.interviewradar.model.dto.StandardQuestionViewDTO;
import com.interviewradar.service.StandardQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/standard-questions")
public class StandardQuestionController {
    private final StandardQuestionService questionService;

    @Autowired
    public StandardQuestionController(StandardQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        System.out.println("进入 /standard-questions，query=" + query);
        Page<StandardQuestionViewDTO> result = questionService.findAll(query, page, size);
        model.addAttribute("page", result);
        model.addAttribute("q", query);
        model.addAttribute("size", size);
        return "standard_questions";
    }
}