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

    boolean existsBySemester_SemesterIdAndTopicCode(Long semesterId, String topicCode);

    @Query("""
    SELECT t FROM Topic t
    WHERE (:semesterId IS NULL OR t.semester.semesterId = :semesterId)
      AND (:kw IS NULL OR :kw = '' OR
           LOWER(t.topicCode) LIKE LOWER(CONCAT('%', :kw, '%')) OR
           LOWER(t.topicName) LIKE LOWER(CONCAT('%', :kw, '%')))
""")
    Page<Topic> search(@Param("semesterId") Long semesterId,
                       @Param("kw") String keyword,
                       Pageable pageable);
}