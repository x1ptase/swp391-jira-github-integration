package com.swp391.backend.service;

import com.swp391.backend.dto.request.CreateTopicRequest;
import com.swp391.backend.dto.request.UpdateTopicRequest;
import com.swp391.backend.dto.response.TopicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TopicService {

    TopicResponse createTopic(CreateTopicRequest request);

    TopicResponse updateTopic(Long id, UpdateTopicRequest request);

    TopicResponse getTopic(Long id);

    Page<TopicResponse> listTopics(String keyword, Pageable pageable);

    void deleteTopic(Long id);
}