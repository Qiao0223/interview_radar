package com.interviewradar.model.repository;

import com.interviewradar.model.entity.CandidateCanonicalQuestionEntity;
import com.interviewradar.model.enums.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateCanonicalQuestionRepository extends JpaRepository<CandidateCanonicalQuestionEntity, Long> {

    // 查找所有未处理的候选问题（用于人工或系统归一流程）
    List<CandidateCanonicalQuestionEntity> findByStatus(CandidateStatus status);

    // 查询某个标准问题下所有已归一进来的候选项
    List<CandidateCanonicalQuestionEntity> findByMatchedCanonical_Id(Long canonicalId);

    // 批量删除指定状态的候选项（如：清理超期的 PENDING）
    void deleteByStatus(CandidateStatus status);
}
