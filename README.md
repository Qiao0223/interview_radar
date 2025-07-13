# 1. 完整工作流程

## 1.1. 增量爬取 & 入库

### 1.1.1. 操作

- 调度器（00:30–08:00）触发 `CrawlerService.crawlNewInterviews()`

- 从牛客接口拉取新面经列表，支持 `contentType=74|250`


### 1.1.2. 数据库变动 (`interview`)

- **插入** 新的 `interview` 记录，字段：

    - `content_id` ← 接口返回的 contentId

    - `title`、`content`、`show_time`、`fetched_at = NOW()`

    - `questions_extracted = FALSE`


---

## 1.2. 原始问题抽取

### 1.2.1. 操作

- 扫描 `interview` 表中所有 `questions_extracted = FALSE` 的记录

- 调用 LLM（`PromptTemplate.QUESTION_EXTRACTION`）提取`raw_question`

- 更新 `interview.questions_extracted = TRUE`


### 1.2.2. 数据库变动 (`raw_question`)

- **插入** 新条目，字段：

    - `interview_id`

    - `question_text`

    - `candidates_generated = FALSE`

    - `categories_assigned = FALSE`

    - `created_at = NOW()`

    - `updated_at = NOW()`


---

## 1.3. 大类分类

### 1.3.1. 操作

- 扫描 `raw_question` 表中所有 `categories_assigned = FALSE` 的记录

- 从 `category` 表加载所有大类标签

- 调用 LLM（`PromptTemplate.QUESTION_CLASSIFICATION`）进行批量分类


### 1.3.2. 数据库变动

- **更新** `raw_question`：

    - `categories_assigned = TRUE`

    - `updated_at = NOW()`

- **插入** 映射 (`raw_question_category`)：

    - `(raw_question_id, category_id, mapped_at = NOW())`


---

## 1.4. 标准化候选生成

### 1.4.1. 操作

- 扫描 `raw_question` 表中所有 `candidates_generated = FALSE` 且 `categories_assigned = TRUE` 的记录

- 批量调用 LLM（`PromptTemplate.QUESTION_STANDARDIZATION`）生成候选标准问法列表

- 提取 LLM 返回的 JSON，得到每条原始问题对应的一个或多个 `titles`


### 1.4.2. 数据库变动 (`standardization_candidate`)

- **插入** 候选：

    - `candidate_text` ← 标准化问题

    - `embedding` ← `EmbeddingService.embedRaw(text)`→JSON

    - `raw_question_id` ← 原始问题 ID

    - `matched_standard_id = NULL`

    - `status = PENDING`

    - `created_at = NOW()`

    - `updated_at = NOW()`

- **更新** 原始问题：

    - `candidates_generated = TRUE`

    - `updated_at = NOW()`


---

## 1.5. 向量检索 & 精判

### 1.5.1. 操作

- 扫描所有 `standardization_candidate` 中 `status = PENDING` 的记录

- 对每条候选：

    1. **向量检索**：全库 Top-K 检索 `standard_question` Collection；

        - 参数：`topK`, `MetricType.L2`, 距离阈值 `≤ threshold`

    2. **构建 Prompt**：列出 `APPROVED, PENDING, REJECTED` 状态的 Top-K 候选及分数

    3. **调用 LLM**：返回 `{"action":"REUSE|CREATE|SKIP","chosenId"?:id}`


### 1.5.2. 数据库变动

| Action     | 操作内容                                                                                                                                             | `candidate_standard.status` |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------- |
| **REUSE**  | 1. `standard_question.usage_count++`, `updated_at = NOW()` 2. 若原标准为 PENDING，则插入缺失 `standard_question_category` 映射 3. 插入 `raw_to_standard_map` 映射 | `MERGED` → `UNDER_REVIEW`   |
| **CREATE** | 1. 新建 `standard_question` (question_text, status=PENDING, count=1, creator=llm, created_at) 2. Milvus 插入新向量 3. 插入 `raw_to_standard_map` 映射       | `PROMOTED` → `UNDER_REVIEW` |
| **SKIP**   | 1. 标记 `candidate_standard.status = SKIPPED` 2. 不插入映射                                                                                             | `SKIPPED` → `UNDER_REVIEW`  |

- **日志**：每次调用 LLM 前后写入 `prompt_log`：

    - `candidate_id, prompt_text, raw_output, timestamp`


---

## 1.6. 人工审核

### 1.6.1. 审核视图

- 查询所有 `candidate_standard.status = UNDER_REVIEW`

- 展示：原文、LLM Prompt & 输出、候选状态、映射详情


### 1.6.2. 审核操作

- **Approve**：`candidate_standard.status = APPROVED`

- **Reject**：`candidate_standard.status = REJECTED`

- **插入** `entity_review_log`：

    - `entity_type = "CANDIDATE"`, `entity_id`, `action`, `reviewer`, `review_comment`, `action_time`


---

## 1.7. 配置 & 可追溯性

- **阈值**：`similarity.threshold`（向量距离）

- **批量大小**：`classification.batch-size`, `standardization.batch-size`

- **日志**：`prompt_log` 保存 LLM 交互，`entity_review_log` 保存审核记录


---

> **备注**：该文档可直接作为 LLM Prompt 或系统设计文档，下次对话请按此流程构建。