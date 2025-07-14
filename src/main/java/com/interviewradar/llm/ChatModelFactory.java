package com.interviewradar.llm;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatModelFactory {

    private final ApplicationContext context;

    public ChatModel get(String beanName) {
        return context.getBean(beanName, ChatModel.class);
    }

    public Map<String, ChatModel> allModels() {
        return context.getBeansOfType(ChatModel.class);
    }
}
