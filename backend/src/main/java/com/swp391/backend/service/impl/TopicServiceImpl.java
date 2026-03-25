package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateTopicRequest;
import com.swp391.backend.dto.request.UpdateTopicRequest;
import com.swp391.backend.dto.response.TopicResponse;
import com.swp391.backend.entity.Semester;
import com.swp391.backend.entity.Topic;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.SemesterRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TopicRepository;
import com.swp391.backend.service.TopicService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final SemesterRepository semesterRepository;

    public TopicServiceImpl(TopicRepository topicRepository,
                            StudentGroupRepository studentGroupRepository,
                            SemesterRepository semesterRepository) {
        this.topicRepository = topicRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.semesterRepository = semesterRepository;
    }

    @Override
    public TopicResponse createTopic(CreateTopicRequest request) {
        String code = safeTrim(request.getTopicCode());
        String name = safeTrim(request.getTopicName());
        String desc = request.getDescription();

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException("Semester not found: " + request.getSemesterId(), 404));

        if (topicRepository.existsBySemester_SemesterIdAndTopicCode(semester.getSemesterId(), code)) {
            throw new BusinessException("Topic code already exists in this semester: " + code, 409);
        }

        Topic topic = new Topic();
        topic.setTopicCode(code);
        topic.setTopicName(name);
        topic.setDescription(desc);
        topic.setSemester(semester);

        try {
            return toResponse(topicRepository.save(topic));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Unique constraint violated (semesterId + topicCode)", 409);
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
            throw new BusinessException("Unique constraint violated", 409);
        }
    }

    @Override
    public TopicResponse getTopic(Long id) {
        return toResponse(getTopicOrThrow(id));
    }

    @Override
    public Page<TopicResponse> listTopics(Long semesterId, String keyword, Pageable pageable) {
        if (semesterId != null && !semesterRepository.existsById(semesterId)) {
            throw new BusinessException("Semester not found: " + semesterId, 404);
        }

        if (StringUtils.hasText(keyword)) {
            keyword = keyword.trim();
        } else {
            keyword = null;
        }

        return topicRepository.search(semesterId, keyword, pageable).map(this::toResponse);
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
        return topicRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Topic not found: " + id, 404));
    }

    private TopicResponse toResponse(Topic t) {
        TopicResponse r = new TopicResponse();
        r.setTopicId(t.getTopicId());
        r.setTopicCode(t.getTopicCode());
        r.setTopicName(t.getTopicName());
        r.setDescription(t.getDescription());

        if (t.getSemester() != null) {
            r.setSemesterId(t.getSemester().getSemesterId());
            r.setSemesterCode(t.getSemester().getSemesterCode());
            r.setSemesterName(t.getSemester().getSemesterName());
        }

        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}