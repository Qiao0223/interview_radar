package com.interviewradar.controller;

import com.interviewradar.service.StandardQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/standard-questions")
public class StandardQuestionController {
    private final StandardQuestionService standardQuestionService;

    @Autowired
    public StandardQuestionController(StandardQuestionService standardQuestionService) {
        this.standardQuestionService = standardQuestionService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("questions", standardQuestionService.findAll());
        return "standard_questions";
    }
}
