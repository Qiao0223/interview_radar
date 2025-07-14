package com.interviewradar.llm;

import com.interviewradar.model.entity.PromptLog;
import com.interviewradar.model.enums.TaskType;
import com.interviewradar.model.repository.PromptLogRepository;
import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LLMLoggingAspect {

    @Autowired
    private PromptLogRepository logRepo;

    @Around("execution(* dev.langchain4j.model.chat.ChatModel.chat(..))")
    public Object logChat(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        String prompt = args != null && args.length > 0 ? args[0].toString() : "";

        long start = System.currentTimeMillis();
        boolean success = true;
        String response = null;
        String error = null;

        try {
            response = (String) pjp.proceed();
            return response;
        } catch (Throwable t) {
            success = false;
            error = t.getMessage();
            throw t;
        } finally {
            long duration = (System.currentTimeMillis() - start);
            PromptContext.Context ctx = PromptContext.get().orElse(new PromptContext.Context(TaskType.UNKNOWN, null));

            PromptLog log = new PromptLog();
            log.setTaskType(ctx.taskType());
            log.setTaskId(ctx.taskId());
            log.setModelName(pjp.getTarget().getClass().getSimpleName());
            log.setPrompt(prompt);
            log.setResponse(response);
            log.setDurationMs((int) duration);
            log.setSuccess(success);
            log.setErrorMessage(error);
            log.setCreatedAt(java.time.LocalDateTime.now());
            log.setUpdatedAt(java.time.LocalDateTime.now());
            logRepo.save(log);

            PromptContext.clear();
        }
    }
}