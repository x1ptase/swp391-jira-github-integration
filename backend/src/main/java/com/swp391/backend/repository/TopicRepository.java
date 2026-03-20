package com.swp391.backend.repository;

import com.swp391.backend.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByTopicCode(String topicCode);

    @Query("SELECT t FROM Topic t " +
            "WHERE (:kw IS NULL OR :kw = '' OR " +
            "LOWER(t.topicCode) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(t.topicName) LIKE LOWER(CONCAT('%', :kw, '%')))")
    Page<Topic> search(@Param("kw") String keyword, Pageable pageable);
}