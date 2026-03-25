package com.swp391.backend.controller;

import com.swp391.backend.dto.request.CreateTopicRequest;
import com.swp391.backend.dto.request.UpdateTopicRequest;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.response.TopicResponse;
import com.swp391.backend.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ApiResponse<Page<TopicResponse>> list(
            @RequestParam(value = "semester_id", required = false) Long semesterId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(topicService.listTopics(semesterId, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<TopicResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(topicService.getTopic(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TopicResponse> create(@Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.success(topicService.createTopic(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TopicResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateTopicRequest request) {
        return ApiResponse.success(topicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ApiResponse.success(null);
    }
}