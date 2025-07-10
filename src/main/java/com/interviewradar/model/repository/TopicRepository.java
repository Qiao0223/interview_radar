package com.interviewradar.model.repository;

import com.interviewradar.model.entity.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

}
