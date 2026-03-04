package com.swp391.backend.service;

import java.util.List;

/**
 * Service for suggesting Jira labels for a group's Jira integration.
 * Jira Cloud does not have a standard "list all labels" endpoint,
 * so labels are aggregated from recent issues.
 */
public interface JiraLabelService {

    /**
     * Aggregate và suggest labels từ issues của project.
     *
     * @param groupId ID của group
     * @param q       prefix/substring filter (case-insensitive), null hoặc blank =
     *                lấy tất cả
     * @param limit   số labels trả về tối đa (clamp 1..100, default 30)
     * @return danh sách label strings (distinct, sorted)
     */
    List<String> suggestLabels(Long groupId, String q, int limit);
}
