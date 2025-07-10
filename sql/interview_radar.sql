/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : interview_radar

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 10/07/2025 16:21:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for canonical_question
-- ----------------------------
DROP TABLE IF EXISTS `canonical_question`;
CREATE TABLE `canonical_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `count` int NOT NULL DEFAULT 1,
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime NULL DEFAULT NULL,
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cq_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_cq_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for canonical_question_topic
-- ----------------------------
DROP TABLE IF EXISTS `canonical_question_topic`;
CREATE TABLE `canonical_question_topic`  (
  `canonical_question_id` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  `mapped_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`canonical_question_id`, `topic_id`) USING BTREE,
  INDEX `idx_cqt_canonical`(`canonical_question_id` ASC) USING BTREE,
  INDEX `idx_cqt_topic`(`topic_id` ASC) USING BTREE,
  CONSTRAINT `fk_cqt_canonical` FOREIGN KEY (`canonical_question_id`) REFERENCES `canonical_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cqt_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '问题分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for entity_review_log
-- ----------------------------
DROP TABLE IF EXISTS `entity_review_log`;
CREATE TABLE `entity_review_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` enum('CANONICAL','TOPIC') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `entity_id` bigint NOT NULL,
  `action` enum('APPROVE','REJECT') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reviewer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `action_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_erl_entity`(`entity_type` ASC, `entity_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for extracted_question
-- ----------------------------
DROP TABLE IF EXISTS `extracted_question`;
CREATE TABLE `extracted_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `interview_id` bigint NOT NULL COMMENT '对应 interviews.content_id，外键',
  `question_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽取出的面试官提问文本',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  `canonicalized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已经对该问题做过标准化',
  `categorized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否对问题进行分类',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_q_interview`(`interview_id` ASC) USING BTREE,
  CONSTRAINT `fk_extracted_question_interview` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`content_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5606 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原始抽取问题表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for extracted_question_canonical
-- ----------------------------
DROP TABLE IF EXISTS `extracted_question_canonical`;
CREATE TABLE `extracted_question_canonical`  (
  `extracted_question_id` bigint NOT NULL,
  `canonical_question_id` bigint NOT NULL,
  `mapped_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`extracted_question_id`, `canonical_question_id`) USING BTREE,
  INDEX `idx_eqc_extracted`(`extracted_question_id` ASC) USING BTREE,
  INDEX `idx_eqc_canonical`(`canonical_question_id` ASC) USING BTREE,
  CONSTRAINT `fk_eqc_canonical` FOREIGN KEY (`canonical_question_id`) REFERENCES `canonical_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_eqc_extracted` FOREIGN KEY (`extracted_question_id`) REFERENCES `extracted_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview
-- ----------------------------
DROP TABLE IF EXISTS `interview`;
CREATE TABLE `interview`  (
  `content_id` bigint NOT NULL COMMENT 'JSON 中的 contentId，全局唯一主键',
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题，对应 momentData.title',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子的完整文本内容',
  `show_time` datetime NULL DEFAULT NULL COMMENT '前端展示时间，对应 momentData.showTime',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '爬取时的时间戳',
  `extracted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已经从原始面经抽取出问题',
  PRIMARY KEY (`content_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面经主表：存储原始帖子信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_to_category
-- ----------------------------
DROP TABLE IF EXISTS `question_to_category`;
CREATE TABLE `question_to_category`  (
  `question_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `mapped_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`question_id`, `category_id`) USING BTREE,
  INDEX `idx_q2c_question`(`question_id` ASC) USING BTREE,
  INDEX `idx_q2c_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_q2c_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_q2c_question` FOREIGN KEY (`question_id`) REFERENCES `extracted_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
