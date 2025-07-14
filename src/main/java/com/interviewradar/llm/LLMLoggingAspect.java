package com.interviewradar.llm;

import com.interviewradar.model.entity.PromptLog;
import com.interviewradar.model.repository.PromptLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class LLMLoggingAspect {

    private final PromptLogRepository promptLogRepository;

    /**
     * 拦截所有 ChatModel.chat(...) 方法调用
     */
    @Pointcut("execution(* dev.langchain4j.model.chat.ChatModel.chat(..))")
    public void chatPointcut() {}

    @Around("chatPointcut()")
    public Object logChat(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;

        // 尝试从参数中获取 prompt 文本
        Object[] args = pjp.getArgs();
        String promptText = args != null && args.length > 0
                ? String.valueOf(args[0])
                : "";

        String response = null;
        try {
            response =(String) pjp.proceed();
            return response;
        } catch (Throwable ex) {
            success = false;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int durationS = (int) (durationMs);

            // 获取 llm 模型信息
            Object target = pjp.getTarget();
            String modelName = target.getClass().getSimpleName();
            String modelVersion = null;

            if (target instanceof MetadataChatModel metadata) {
                modelName = metadata.getModelName();
                modelVersion = metadata.getModelVersion();
            }

            // 只有在 ThreadLocal 中存在上下文时才记录
            List<PromptContext.Context> contexts = PromptContext.getAll();
            if (!contexts.isEmpty()) {
                for (PromptContext.Context ctx : contexts) {
                    PromptLog log = PromptLog.builder()
                            .taskType(ctx.taskType())
                            .taskId(ctx.taskId())
                            .prompt(promptText)
                            .response(response)
                            .modelName(modelName)
                            .modelVersion(modelVersion)
                            .durationMs(durationS)
                            .success(success)
                            .errorMessage(errorMsg)
                            .build();
                    promptLogRepository.save(log);
                }
                // 清理，避免影响下次调用
                PromptContext.clear();
            }
        }
    }
}
