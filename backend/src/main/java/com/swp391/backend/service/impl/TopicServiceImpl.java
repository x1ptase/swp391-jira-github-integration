package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateTopicRequest;
import com.swp391.backend.dto.request.UpdateTopicRequest;
import com.swp391.backend.dto.response.TopicResponse;
import com.swp391.backend.entity.Topic;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TopicRepository;
import com.swp391.backend.service.TopicService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final StudentGroupRepository studentGroupRepository;

    public TopicServiceImpl(TopicRepository topicRepository,
                            StudentGroupRepository studentGroupRepository) {
        this.topicRepository = topicRepository;
        this.studentGroupRepository = studentGroupRepository;
    }

    @Override
    public TopicResponse createTopic(CreateTopicRequest request) {
        String code = safeTrim(request.getTopicCode());
        String name = safeTrim(request.getTopicName());
        String desc = request.getDescription();

        if (topicRepository.existsByTopicCode(code)) {
            throw new BusinessException("Topic code already exists: " + code, 409);
        }

        Topic topic = new Topic();
        topic.setTopicCode(code);
        topic.setTopicName(name);
        topic.setDescription(desc);

        try {
            return toResponse(topicRepository.save(topic));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Unique constraint violated (topicCode/topicName)", 409);
        }
    }

    @Override
    public TopicResponse updateTopic(Long id, UpdateTopicRequest request) {
        Topic topic = getTopicOrThrow(id);

        String newName = safeTrim(request.getTopicName());

        topic.setTopicName(newName);
        topic.setDescription(request.getDescription());

        try {
            return toResponse(topicRepository.save(topic));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Unique constraint violated (topicName)", 409);
        }
    }

    @Override
    public TopicResponse getTopic(Long id) {
        return toResponse(getTopicOrThrow(id));
    }

    @Override
    public Page<TopicResponse> listTopics(String keyword, Pageable pageable) {
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) keyword = null;
        }
        return topicRepository.search(keyword, pageable).map(this::toResponse);
    }

    @Override
    public void deleteTopic(Long id) {
        Topic topic = getTopicOrThrow(id);

        boolean usedByGroup = studentGroupRepository.existsByTopic_TopicId(id);
        if (usedByGroup) {
            throw new BusinessException("Cannot delete topic because it is assigned to a group", 409);
        }

        try {
            topicRepository.delete(topic);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Cannot delete topic because it is referenced by other records", 409);
        }
    }

    private Topic getTopicOrThrow(Long id) {
        Optional<Topic> opt = topicRepository.findById(id);
        if (!opt.isPresent()) {
            throw new BusinessException("Topic not found: " + id, 404);
        }
        return opt.get();
    }

    private TopicResponse toResponse(Topic t) {
        TopicResponse r = new TopicResponse();
        r.setTopicId(t.getTopicId());
        r.setTopicCode(t.getTopicCode());
        r.setTopicName(t.getTopicName());
        r.setDescription(t.getDescription());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    private String safeTrim(String s) {
        if (s == null) return "";
        return s.trim();
    }
}