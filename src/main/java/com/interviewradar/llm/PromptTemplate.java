package com.interviewradar.llm;

public enum PromptTemplate {

    // 1. 问题抽取模板
    QUESTION_EXTRACTION("""
            你是一个面试题抽取专家。 \s
            接下来给你一段「面经原文」，其中可能包含面试官提问、面试者回答、自述感受、无关叙述等各种内容。 \s
            你的任务是：**只抽取真正的「面试官提问」**，不需要按编号或问号硬匹配，而是结合语义和上下文判断哪些句子/短语是在问问题。
            请遵守以下规则：
            一、语义识别：\s
            1. 抽取要求解释、描述、分析、判断、解决等开放式问题（如：“解释……”、“如何设计……”） \s
            2. 命令式语句也视为隐含问题（如“自我介绍” → “请做自我介绍”） \s
            3. 忽略面试者感受（如“感觉被拷打了”）、流程描述（如“最后是反问环节”）以及非问题性陈述
            4. 忽略面试管询问的个人问题(家庭情况,是否接受公司条件等等)
            二、文本处理：\s
            1. 将多个关联问题合并成一个（如“线程池参数 + 拒绝策略”） \s
            2. 删除无关修饰（如“巴拉巴拉”、“很简单”），但保留关键词
            3. 上下文强关联合并成一个问题
            三、输出格式：\s
            1. 输出一个 JSON 对象，字段名为 `questions`，值是一个字符串数组 \s
            2. 每个元素是一条干净的问题文本，不要带编号、问号、上下文说明 \s
            3. 保持问题的原始表述，仅做必要的清理
            示例：
            输入：
            ```
            丸辣，被JVM背刺了 \s
            问GC算法区别 \s
            项目中的难点怎么解决 \s
            手撕二叉树层序遍历 \s
            ```
            预期输出：
            ```json
            {
              "questions": [
                "GC算法区别",
                "项目中的难点怎么解决",
                "二叉树层序遍历"
              ]
            }
            ```
            现在，请根据以上说明，处理下面这段面经原文，返回只包含 `questions` 数组的 JSON：
            {{rawInterview}}
    """),

    // 2. 问题分类模板
    QUESTION_CLASSIFICATION("""
    你是一个面试题分类助手。
    下面有 ${n} 条面试问题，请对每条问题分别从以下分类列表中选择最贴切的分类编号（可多选）。
    返回一个 JSON 数组，每一项格式为：
    {
      "index": <问题在本批中的序号，从1开始>,
      "categories": [<分类编号1>,<分类编号2>,…]
    }
    
    分类列表（编号→名称（说明））：
    ${categories}
    
    问题列表：
    1. {questionText1}
    2. {questionText2}
    …
    N. {questionTextN}
    index 用于关联批量请求里的具体哪条问题
    返回举例：
    [
      {"index":1,"categories":[2,5]},
      {"index":2,"categories":[3]},
      …
    ]
    """),


    // 3. 知识点抽取模板
    KNOWLEDGE_EXTRACTION("""
      你是一个面试题知识点抽取助手。
      给定一条已分类的面试问题，请结合其分类上下文，从已有知识点库中提取相关知识点名称。
      如果提取的知识点不在已有库中，也请输出。

      问题文本：
      {questionText}
      已选分类：{categoryName}

      输出格式为 JSON 数组，例如 ["事务隔离级别", "MVCC"]。
    """),

    ;

    private final String template;
    PromptTemplate(String template) { this.template = template; }
    public String getTemplate() { return template; }
}
