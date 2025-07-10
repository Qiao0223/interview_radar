# 1. 完整工作流程

## 1.1. 增量爬取 & 入库

### 1.1.1. 操作

- 调度器（00:30–08:00）触发 `CrawlerService.fetchAndSaveNewInterviews()`
- 从牛客接口拉取新面经列表

### 1.1.2. 数据库变动 (`interview`)

- **插入** 新的 `interview` 记录，字段：  
  - `content_id` ← 接口返回的 contentId  
  - `title`、`content`、`show_time`、`fetched_at = NOW()`  
  - `extracted = FALSE`

---

## 1.2. 原始问题抽取

### 1.2.1. 操作

- 扫描 `interview` 表中所有 `extracted = FALSE` 的记录  
- 调用 LLM 提取原始问句，生成 `extracted_question.question_text`  
- 更新 `interview.extracted = TRUE`

### 1.2.2. 数据库变动 (`extracted_question`)

- **插入** 新条目，字段：  
  - `interview_id`  
  - `question_text`  
  - `canonicalized = FALSE`  
  - `categorized = FALSE`  
  - `created_at = NOW()`  
  - `updated_at = NOW()`

---

## 1.3. 大类分类

### 1.3.1. 操作

- 扫描所有 `extracted_question` 中 `categorized = FALSE` 的记录  
- 从 `category` 表加载所有大类标签  
- 调用 LLM 对 `extracted_question.question_text` 进行分类

### 1.3.2. 数据库变动

- **更新** `extracted_question`：  
  - `categorized = TRUE`  
  - `updated_at = NOW()`  
- **插入** 映射 (`question_to_category`)：  
  - `(question_id, category_id, mapped_at = NOW())`

> *本阶段仅修改关系库，不涉及向量库。*

---

## 1.4. 向量化 & 候选召回

### 1.4.1. 操作

- 对所有已分类 (`categorized = TRUE`) 且在 `question_to_category` 中存在有效映射的 `extracted_question`  
- 调用 `EmbeddingService.embed(extracted_question.question_text)` → `question_embedding`  
- 在 Milvus 的 `canonical_question` Collection 上以 `category_id == X` 过滤做 Top-K 检索，返回候选

> *本阶段仅触发向量库的检索，关系库无变动。*

---

## 1.5. 标准化问法精判

### 1.5.1. 操作

- 从检索结果中取回候选 `(id, text, status, score)`  
- 拼装 Prompt，让 LLM 在以下三种操作中决策（若有 `status = REJECTED`，带上对应的 `review_comment`）：
  1. **复用**：选中某个 `status ∈ {PENDING, APPROVED}`  
  2. **跳过**：Top-1 为 `REJECTED`  
  3. **新建**：所有候选均不合适

### 1.5.2. 数据库变动

#### 1.5.2.1 复用分支

- **更新** `canonical_question`：  
  - `count = count + 1`  
  - `updated_at = NOW()`  
- **插入** 映射 (`extracted_question_canonical`)：  
  - `(extracted_question_id, canonical_question_id, mapped_at = NOW())`  
- **更新** `extracted_question`：  
  - `canonicalized = TRUE`  
  - `updated_at = NOW()`

#### 1.5.2.2 跳过分支

- **无** 关系库变动  
- 保持 `extracted_question.canonicalized = FALSE`

#### 1.5.2.3 新建分支

- **插入** 新 `canonical_question`：  
  - `text` ← LLM 生成的标准化问法  
  - `category_id` ← 原始 `extracted_question` 对应 `category_id`  
  - `status = 'PENDING'`  
  - `count = 1`  
  - `created_by`、`created_at = NOW()`  
- **调用** `EmbeddingService.embed(canonical_question.text)` → `canonical_question_embedding`  
- **Upsert** 到 Milvus 的 `canonical_question` Collection：  
  ```text
  milvus.upsert("canonical_question", {
    id: canonical_question.id,
    text: canonical_question.text,
    question_embedding: canonical_question_embedding,
    category_id: canonical_question.category_id,
    status: status_code
  })
  ```  
- **插入** 映射 (`extracted_question_canonical`)：  
  - `(extracted_question_id, canonical_question_id, mapped_at = NOW())`  
- **更新** `extracted_question`：  
  - `canonicalized = TRUE`  
  - `updated_at = NOW()`

---

## 1.6. 知识点映射

### 1.6.1. 操作

- 针对所有 `canonical_question` 中 `status ∈ {PENDING, APPROVED}` 且 **未映射** 的记录  
- 调用 LLM 从 `canonical_question.text` 生成零或多个知识点 `topic.name` 和可选 `topic.description`

### 1.6.2. 数据库变动

#### 1.6.2.1 复用分支

- **更新** `topic`：  
  - `occurrence_count = occurrence_count + canonical_question.count`  
  - `updated_at = NOW()`  
- **插入** 映射 (`canonical_question_topic`)：  
  - `(canonical_question_id, topic_id, mapped_at = NOW())`  
- **更新** `canonical_question`：  
  - `topic_mapping_status = 'MAPPED'`  
  - `updated_at = NOW()`

#### 1.6.2.2 新建分支

- **插入** 新 `topic`：  
  - `name` ← LLM 生成  
  - `description` ← LLM 生成或留空  
  - `category_id` ← 对应大类  
  - `status = 'PENDING'`  
  - `occurrence_count = 1`  
  - `created_by`、`created_at = NOW()`  
- **插入** 映射 (`canonical_question_topic`)：  
  - `(canonical_question_id, topic_id, mapped_at = NOW())`  
- **调用** `EmbeddingService.embed(...)` 对 `topic.description` 按段切片  
- **Upsert** 每段到 Milvus 的 `topic_chunk` Collection  
- **更新** `topic_mapping_status = 'MAPPED'`

---

## 1.7. 审核与回流

- 在后台 UI 人工审核所有 `canonical_question.status = 'PENDING'` 和 `topic.status = 'PENDING'`  
- **审核通过**：  
  - **更新** 对象状态 → `status = 'APPROVED'`、`reviewed_by`、`reviewed_at = NOW()`  
  - **批量 Upsert** 已批准的 `canonical_question` 到 Milvus  
  - **批量 Upsert** 已批准的 `topic_chunk` 到 Milvus  
- **审核驳回**：  
  - **更新** `status = 'REJECTED'`、填写 `review_comment`  
  - 若驳回的是标准化问法，则后续同义抽取 Prompt 中提示“请避免重复此问题”  
  - 若驳回的是知识点，则对应 `canonical_question.topic_mapping_status = 'UNMAPPED'`，重新进入知识点映射流程

---

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
