package com.interviewradar.llm;

import com.interviewradar.model.enums.TaskType;

import java.util.Optional;

public class PromptContext {
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    public static void set(TaskType taskType, Long taskId) {
        CONTEXT.set(new Context(taskType, taskId));
    }

    public static Optional<Context> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record Context(TaskType taskType, Long taskId) {}
}