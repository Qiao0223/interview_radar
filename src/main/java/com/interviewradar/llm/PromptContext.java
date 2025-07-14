package com.interviewradar.llm;

import com.interviewradar.model.enums.TaskType;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


public class PromptContext {

    private static final ThreadLocal<List<Context>> CONTEXTS =
            ThreadLocal.withInitial(ArrayList::new);

    public static void add(TaskType taskType, Long taskId) {
        CONTEXTS.get().add(new Context(taskType, taskId));
    }

    public static void addBatch(TaskType taskType, List<Long> taskIds) {
        List<Context> list = CONTEXTS.get();
        taskIds.forEach(id -> list.add(new Context(taskType, id)));
    }

    public static List<Context> getAll() {
        return Collections.unmodifiableList(CONTEXTS.get());
    }

    public static void clear() {
        CONTEXTS.remove();
    }

    public record Context(TaskType taskType, Long taskId) {}
}

