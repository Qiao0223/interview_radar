# Interview Radar

This project extracts interview questions from forums, classifies them and stores structured data for later retrieval.  
The workflow below summarizes how data moves through the system. The details come from `interview_workflow.md`.

## Workflow summary

1. **Crawling** – A scheduled job calls `CrawlerService.fetchAndSaveNewInterviews()` to pull new posts and insert records into the `interview` table with `extracted = FALSE`.
2. **Question Extraction** – For every unprocessed interview the system invokes the LLM to extract the original questions. New rows are inserted into `extracted_question` and the interview flag is updated.
3. **Category Classification** – Unclassified questions are batched and sent to the LLM together with the list of available categories. Results are written back via the `question_to_category` table and the question row is marked `categorized = TRUE`.
4. **Vector Embedding & Candidate Recall** – Classified questions are embedded and searched against the Milvus collection `canonical_question` filtered by category id.
5. **Canonical Question Decision** – Depending on LLM evaluation the extracted question either reuses an existing canonical form, is skipped or creates a new canonical question. Mapping information is stored in `extracted_question_canonical`.
6. **Topic Mapping** – Canonical questions without topics trigger another LLM call which either links to an existing topic or creates a new one. Topics may also be embedded and stored in the Milvus `topic_chunk` collection.
7. **Review & Feedback** – Pending canonical questions and topics are manually reviewed. Actions are logged to `entity_review_log` and approved entries are upserted to Milvus.

The database schema used by this project can be found in [`sql/interview_radar.sql`](sql/interview_radar.sql) and a more detailed description of each step lives in [`interview_workflow.md`](interview_workflow.md).

## Building

The project is a standard Spring Boot application. Use the Maven wrapper to build:

```bash
./mvnw clean package
```

A `docker-compose.yml` is included for running a Milvus instance locally.
