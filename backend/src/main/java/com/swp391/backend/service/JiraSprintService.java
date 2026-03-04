package com.swp391.backend.service;

import com.swp391.backend.dto.response.JiraSprintResponse;

import java.util.List;

/**
 * Service for listing Jira sprints for a group's Jira integration.
 */
public interface JiraSprintService {

    /**
     * Lấy danh sách sprints của project được cấu hình cho group.
     *
     * @param groupId ID của group
     * @param state   state filter: "active", "future", "closed" hoặc
     *                comma-separated
     *                (default: "active,future,closed")
     * @return danh sách sprints
     */
    List<JiraSprintResponse> listSprints(Long groupId, String state);
}
