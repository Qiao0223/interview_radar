/*
 Navicat Premium Dump SQL

 Source Server         : 阿康
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : 115.190.83.184:3306
 Source Schema         : interview_radar

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 13/07/2025 14:34:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键, 0:不相关,1:Java基础,2:JVM原理,3:并发/多线程,4:Spring生态,5:设计模式,6:微服务,7:分布式系统,8:消息队列,9:计算机网络,10:操作系统,11:算法与数据结构,12:MySQL,13:Redis,14:其他数据库,15:AI相关,16:场景题,17:项目类,18:软技能,20:其他',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称，如 JVM、数据库、网络 等',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分类创建时间',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '问题分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for entity_review_log
-- ----------------------------
DROP TABLE IF EXISTS `entity_review_log`;
CREATE TABLE `entity_review_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `entity_id` bigint NOT NULL,
  `action` enum('APPROVE','REJECT') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reviewer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `review_comment` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `action_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_erl_entity`(`entity_type` ASC, `entity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prompt_log
-- ----------------------------
DROP TABLE IF EXISTS `prompt_log`;
CREATE TABLE `prompt_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `candidate_id` bigint NOT NULL,
  `prompt_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `response_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `candidate_id`(`candidate_id` ASC) USING BTREE,
  CONSTRAINT `prompt_log_ibfk_1` FOREIGN KEY (`candidate_id`) REFERENCES `standardization_candidate` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for raw_interview
-- ----------------------------
DROP TABLE IF EXISTS `raw_interview`;
CREATE TABLE `raw_interview`  (
  `content_id` bigint NOT NULL COMMENT 'JSON 中的 contentId，全局唯一主键',
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题，对应 momentData.title',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子的完整文本内容',
  `show_time` datetime NULL DEFAULT NULL COMMENT '前端展示时间，对应 momentData.showTime',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '爬取时的时间戳',
  `questions_extracted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '已抽取问句',
  PRIMARY KEY (`content_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面经主表：存储原始帖子信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for raw_question
-- ----------------------------
DROP TABLE IF EXISTS `raw_question`;
CREATE TABLE `raw_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `interview_id` bigint NOT NULL COMMENT '对应 interviews.content_id，外键',
  `question_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽取出的面试官提问文本',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  `candidates_generated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '已生成标准化候选',
  `categories_assigned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '已分配类别',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_q_interview`(`interview_id` ASC) USING BTREE,
  CONSTRAINT `fk_extracted_question_interview` FOREIGN KEY (`interview_id`) REFERENCES `raw_interview` (`content_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原始抽取问题表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for raw_question_category
-- ----------------------------
DROP TABLE IF EXISTS `raw_question_category`;
CREATE TABLE `raw_question_category`  (
  `raw_question_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `assigned_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`raw_question_id`, `category_id`) USING BTREE,
  INDEX `idx_q2c_question`(`raw_question_id` ASC) USING BTREE,
  INDEX `idx_q2c_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_q2c_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_q2c_question` FOREIGN KEY (`raw_question_id`) REFERENCES `raw_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for raw_to_standard_map
-- ----------------------------
DROP TABLE IF EXISTS `raw_to_standard_map`;
CREATE TABLE `raw_to_standard_map`  (
  `raw_question_id` bigint NOT NULL,
  `standard_question_id` bigint NOT NULL,
  `mapped_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`raw_question_id`, `standard_question_id`) USING BTREE,
  INDEX `idx_eqc_extracted`(`raw_question_id` ASC) USING BTREE,
  INDEX `idx_eqc_canonical`(`standard_question_id` ASC) USING BTREE,
  CONSTRAINT `fk_eqc_canonical` FOREIGN KEY (`standard_question_id`) REFERENCES `standard_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_eqc_extracted` FOREIGN KEY (`raw_question_id`) REFERENCES `raw_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for standard_question
-- ----------------------------
DROP TABLE IF EXISTS `standard_question`;
CREATE TABLE `standard_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `usage_count` int NOT NULL DEFAULT 1,
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime NULL DEFAULT NULL,
  `review_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `embedding` json NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for standard_question_category
-- ----------------------------
DROP TABLE IF EXISTS `standard_question_category`;
CREATE TABLE `standard_question_category`  (
  `standard_question_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`standard_question_id`, `category_id`) USING BTREE,
  INDEX `category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `standard_question_category_ibfk_1` FOREIGN KEY (`standard_question_id`) REFERENCES `standard_question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `standard_question_category_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for standardization_candidate
-- ----------------------------
DROP TABLE IF EXISTS `standardization_candidate`;
CREATE TABLE `standardization_candidate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `candidate_text` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `embedding` json NULL,
  `raw_question_id` bigint NOT NULL,
  `matched_standard_id` bigint NULL DEFAULT NULL,
  `status` enum('PENDING','MERGED','PROMOTED','SKIPPED','UNDER_REVIEW') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `source_question_id`(`raw_question_id` ASC) USING BTREE,
  INDEX `matched_canonical_id`(`matched_standard_id` ASC) USING BTREE,
  CONSTRAINT `standardization_candidate_ibfk_1` FOREIGN KEY (`raw_question_id`) REFERENCES `raw_question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `standardization_candidate_ibfk_2` FOREIGN KEY (`matched_standard_id`) REFERENCES `standard_question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7167 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for topic
-- ----------------------------
DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `category_id` bigint NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `occurrence_count` int NOT NULL DEFAULT 0,
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime NULL DEFAULT NULL,
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_topic_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_topic_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
