package com.swp391.backend.service;

import com.swp391.backend.dto.response.JiraVersionResponse;

import java.util.List;

/**
 * Service for listing Jira project versions for a group's Jira integration.
 */
public interface JiraVersionService {

    /**
     * Lấy danh sách versions của project được cấu hình cho group.
     *
     * @param groupId         ID của group
     * @param includeArchived có bao gồm archived versions không (default false)
     * @param includeReleased có bao gồm released versions không (default true)
     * @return danh sách versions sau khi filter
     */
    List<JiraVersionResponse> listVersions(Long groupId, boolean includeArchived, boolean includeReleased);
}
