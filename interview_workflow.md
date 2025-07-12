# 1. 完整工作流程

## 1.1. 增量爬取 & 入库

### 1.1.1. 操作

* 调度器（00:30–08:00）触发 `CrawlerService.crawlNewInterviews()`
* 从牛客接口拉取新面经列表，支持 `contentType=74|250`

### 1.1.2. 数据库变动 (`interview`)

* **插入** 新的 `interview` 记录，字段：

  * `content_id` ← 接口返回的 contentId
  * `title`、`content`、`show_time`、`fetched_at = NOW()`
  * `extracted = FALSE`

---

## 1.2. 原始问题抽取

### 1.2.1. 操作

* 扫描 `interview` 表中所有 `extracted = FALSE` 的记录
* 调用 LLM（`PromptTemplate.QUESTION_EXTRACTION`）提取原始问句
* 更新 `interview.extracted = TRUE`

### 1.2.2. 数据库变动 (`raw_question`)

* **插入** 新条目，字段：

  * `interview_id`
  * `question_text`
  * `canonicalized = FALSE`
  * `categorized = FALSE`
  * `created_at = NOW()`
  * `updated_at = NOW()`

---

## 1.3. 大类分类

### 1.3.1. 操作

* 扫描 `raw_question` 表中所有 `categorized = FALSE` 的记录
* 从 `category` 表加载所有大类标签
* 调用 LLM（`PromptTemplate.QUESTION_CLASSIFICATION`）进行批量分类

### 1.3.2. 数据库变动

* **更新** `raw_question`：

  * `categorized = TRUE`
  * `updated_at = NOW()`
* **插入** 映射 (`raw_question_category`)：

  * `(question_id, category_id, mapped_at = NOW())`

---

## 1.4. 标准化候选生成

### 1.4.1. 操作

* 扫描 `raw_question` 表中所有 `canonicalized = FALSE` 且 `categorized = TRUE` 的记录
* 批量调用 LLM（`PromptTemplate.QUESTION_STANDARDIZATION`）生成候选标准问法列表
* 提取 LLM 返回的 JSON，得到每条原始问题对应的一个或多个 `titles`

### 1.4.2. 数据库变动 (`candidate_standard`)

* **插入** 候选：

  * `text` ← 标准化标题
  * `embedding` ← `EmbeddingService.embedRaw(text)`→JSON
  * `source_question_id` ← 原始问题 ID
  * `matched_standard_id = NULL`
  * `status = PENDING`
  * `created_at = NOW()`
* **更新** 原始问题：

  * `canonicalized = TRUE`
  * `updated_at = NOW()`

---

## 1.5. 向量检索 & 精判

### 1.5.1. 操作

* 扫描所有 `candidate_standard` 中 `status = PENDING` 的记录
* 对每条候选：

  1. **向量检索**：全库 Top-K 检索 `standard_question` Collection；

    * 参数：`topK`, `MetricType.L2`, 距离阈值 `≤ threshold`
  2. **构建 Prompt**：列出 `APPROVED, PENDING, REJECTED` 状态的 Top-K 候选及分数
  3. **调用 LLM**：返回 `{"action":"REUSE|CREATE|SKIP","chosenId"?:id}`

### 1.5.2. 数据库变动

| Action     | 操作内容                                                                                                                                                   | `candidate_standard.status` |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------- |
| **REUSE**  | 1. `standard_question.count++`, `updated_at = NOW()`  <br> 2. 若原标准为 PENDING，则插入缺失 `standard_question_category` 映射  <br> 3. 插入 `raw_to_standard_map` 映射 | `MERGED` → `UNDER_REVIEW`   |
| **CREATE** | 1. 新建 `standard_question` (text, status=PENDING, count=1, createdBy=system, timestamps)  <br> 2. Milvus 插入新向量  <br> 3. 插入 `raw_to_standard_map` 映射     | `PROMOTED` → `UNDER_REVIEW` |
| **SKIP**   | 1. 标记 `candidate_standard.status = SKIPPED`  <br> 2. （可选）不插入映射                                                                                         | `SKIPPED` → `UNDER_REVIEW`  |

* **日志**：每次调用 LLM 前后写入 `prompt_log`：

  * `candidate_id, prompt_text, raw_output, timestamp`

---

## 1.6. 人工审核

### 1.6.1. 审核视图

* 查询所有 `candidate_standard.status = UNDER_REVIEW`
* 展示：原文、LLM Prompt & 输出、候选状态、映射详情

### 1.6.2. 审核操作

* **Approve**：`candidate_standard.status = APPROVED`
* **Reject**：`candidate_standard.status = REJECTED`
* **插入** `entity_review_log`：

  * `entity_type = "CANDIDATE"`, `entity_id`, `action`, `reviewer`, `review_comment`, `action_time`

---

## 1.7. 配置 & 可追溯性

* **阈值**：`similarity.threshold`（向量距离）
* **批量大小**：`classification.batch-size`, `standardization.batch-size`
* **日志**：`prompt_log` 保存 LLM 交互，`entity_review_log` 保存审核记录

---

> **备注**：该文档可直接作为 LLM Prompt 或系统设计文档，下次对话请按此流程构建。


# 2. 关系数据库表设计

## 2.1. `interview`

| 列名         | 类型        | 含义              |
| ------------ | ----------- | ----------------- |
| `content_id` | BIGINT      | 主键              |
| `title`      | VARCHAR(512)| 帖子标题          |
| `content`    | TEXT        | 帖子正文          |
| `show_time`  | DATETIME    | 前端展示时间      |
| `fetched_at` | DATETIME    | 爬取时间          |
| `extracted`  | TINYINT(1)  | 是否已抽取问题 (0/1)|

## 2.2. `extracted_question`

| 列名              | 类型         | 含义               |
| --------------- | ---------- | ---------------- |
| `id`            | BIGINT     | 自增主键             |
| `interview_id`  | BIGINT     | 外键 → `interview` |
| `question_text` | TEXT       | 抽取出的原始问句         |
| `canonicalized` | TINYINT(1) | 是否已标准化 (0/1)     |
| `categorized`   | TINYINT(1) | 是否已分类 (0/1)      |
| `created_at`    | DATETIME   | 插入时间             |
| `updated_at`    | DATETIME   | 更新时间             |

## 2.3. `category`

| 列名        | 类型         | 含义       |
| ----------- | ------------ | ---------- |
| `id`        | BIGINT       | 自增主键    |
| `name`      | VARCHAR(64)  | 分类名称    |
| `created_at`| DATETIME     | 创建时间    |
| `updated_at`| DATETIME     | 更新时间    |

## 2.4. `question_to_category`

| 列名           | 类型      | 含义                |
| -------------- | --------- | ------------------- |
| `question_id`  | BIGINT    | 原始问题 ID         |
| `category_id`  | BIGINT    | 分类 ID             |
| `mapped_at`    | DATETIME  | 映射时间            |

## 2.5. `canonical_question`

| 列名           | 类型                                 | 含义             |
| -------------- | ------------------------------------ | ---------------- |
| `id`           | BIGINT                               | 自增主键          |
| `text`         | TEXT                                 | 标准化问法原文    |
| `category_id`  | BIGINT                               | 大类 ID          |
| `status`       | ENUM('PENDING','APPROVED','REJECTED')| 审核状态          |
| `count`        | INT                                  | 被复用次数        |
| `created_by`   | VARCHAR(50)                          | 创建者           |
| `created_at`   | DATETIME                             | 创建时间         |
| `reviewed_by`  | VARCHAR(50)                          | 审核者           |
| `reviewed_at`  | DATETIME                             | 审核时间         |
| `review_comment`| TEXT                                | 审核备注         |
| `updated_at`   | DATETIME                             | 更新时间         |

## 2.6. `extracted_question_canonical`

| 列名                     | 类型      | 含义                       |
| ------------------------ | --------- | -------------------------- |
| `extracted_question_id`  | BIGINT    | 原始问句 ID                |
| `canonical_question_id`  | BIGINT    | 标准化问法 ID              |
| `mapped_at`              | DATETIME  | 映射时间                   |

## 2.7. `topic`

| 列名               | 类型                                 | 含义              |
| ------------------ | ------------------------------------ | ----------------- |
| `id`               | BIGINT                               | 自增主键           |
| `name`             | VARCHAR(128)                         | 知识点名称         |
| `description`      | TEXT                                 | 知识点描述         |
| `category_id`      | BIGINT                               | 大类 ID           |
| `status`           | ENUM('PENDING','APPROVED','REJECTED')| 审核状态           |
| `occurrence_count` | INT                                  | 复用次数           |
| `created_by`       | VARCHAR(50)                          | 创建者           |
| `created_at`       | DATETIME                             | 创建时间         |
| `reviewed_by`      | VARCHAR(50)                          | 审核者           |
| `reviewed_at`      | DATETIME                             | 审核时间         |
| `review_comment`   | TEXT                                 | 审核备注           |
| `updated_at`       | DATETIME                             | 更新时间         |

## 2.8. `canonical_question_topic`

| 列名                    | 类型      | 含义                   |
| ----------------------- | --------- | ---------------------- |
| `canonical_question_id` | BIGINT    | 标准化问法 ID          |
| `topic_id`              | BIGINT    | 知识点 ID              |
| `mapped_at`             | DATETIME  | 映射时间               |

## 2.9. `entity_review_log`

| 列名               | 类型                        | 含义      |
| ---------------- | ------------------------- | ------- |
| `id`             | BIGINT                    | 自增主键    |
| `entity_type`    | ENUM('CANONICAL','TOPIC') | 审核对象类型  |
| `entity_id`      | BIGINT                    | 审核对象 ID |
| `action`         | ENUM('APPROVE','REJECT')  | 审核动作    |
| `reviewer`       | VARCHAR(100)              | 审核者     |
| `review_comment` | TEXT                      | 审核备注    |
| `action_time`    | DATETIME                  | 操作时间    |

---

# 3. Milvus 向量库 Collection 设计

## 3.1. `canonical_question` Collection

| 字段                  | 类型                        | 说明                                   |
| --------------------- | --------------------------- | -------------------------------------- |
| `id`                  | `Int64`                     | 对应关系库 `canonical_question.id`    |
| `text`                | `VarChar(256)`              | 标准化问法原文                         |
| `question_embedding`  | `FloatVector(dim=embDim)`   | 向量表示                               |
| `category_id`         | `Int64`                     | 大类 ID                                |
| `status`              | `Int32`                     | 0 = PENDING, 1 = APPROVED, 2 = REJECTED |

## 3.2. `topic_chunk` Collection

| 字段                  | 类型                         | 说明                                   |
| --------------------- | ---------------------------- | -------------------------------------- |
| `chunk_id`            | `Int64`                      | 主键                                   |
| `topic_id`            | `Int64`                      | 对应 `topic.id`                        |
| `paragraph_idx`       | `Int32`                      | 分片序号                               |
| `chunk_text`          | `VarChar(2048)`              | 分片文本                               |
| `chunk_embedding`     | `FloatVector(dim=embDim)`    | 向量表示                               |
| `category_id`         | `Int64`                      | 大类 ID                                |
| `status`              | `Int32`                     | 0 = PENDING, 1 = APPROVED, 2 = REJECTED |
