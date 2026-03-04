package com.swp391.backend.service;

import com.swp391.backend.dto.response.JiraIssueExportDto;
import com.swp391.backend.dto.response.JiraIssuePageResponse;
import com.swp391.backend.integration.jira.JiraJqlBuilder.FilterType;

import java.util.List;

/**
 * Service for fetching Jira issues using the new Cloud APIs:
 * - GET /rest/api/3/search/jql (issue IDs + nextPageToken)
 * - POST /rest/api/3/issue/bulkfetch (full issue details)
 */
public interface JiraIssueService {

        /**
         * Fetches all Jira issues (fetchAll=true): loops over pages until isLast=true.
         * Returns flat list.
         *
         * @param groupId    ID của group
         * @param filterType loại filter (ALL / SPRINT / VERSION / LABEL)
         * @param sprintId   Sprint ID (required when filterType=SPRINT)
         * @param versionId  Version ID / name (required when filterType=VERSION)
         * @param label      Label (required when filterType=LABEL)
         * @param maxResults page size (clamped 1..100)
         * @return all issues as flat list
         */
        List<JiraIssueExportDto> fetchAllIssues(Long groupId, FilterType filterType,
                        Long sprintId, String versionId, String label,
                        int maxResults);

        /**
         * Fetches a single page of Jira issues (fetchAll=false).
         * Returns items + nextPageToken + isLast so FE can paginate.
         *
         * @param groupId    ID của group
         * @param filterType loại filter
         * @param sprintId   Sprint ID (required when filterType=SPRINT)
         * @param versionId  Version ID (required when filterType=VERSION)
         * @param label      Label (required when filterType=LABEL)
         * @param pageToken  opaque token from previous response (null for first page)
         * @param maxResults page size (clamped 1..100)
         * @return single page with pagination metadata
         */
        JiraIssuePageResponse fetchIssuePage(Long groupId, FilterType filterType,
                        Long sprintId, String versionId, String label,
                        String pageToken, int maxResults);
}
