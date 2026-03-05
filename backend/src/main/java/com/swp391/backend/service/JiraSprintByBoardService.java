package com.swp391.backend.service;

import com.swp391.backend.dto.response.JiraSprintDto;

import java.util.List;

/**
 * Service to list Jira sprints for a specific board within a group's Jira
 * project.
 * Unlike {@link JiraSprintService}, the caller explicitly provides the boardId
 * instead of letting the service auto-pick the first board.
 */
public interface JiraSprintByBoardService {

    /**
     * Returns sprints for the given board, filtered by state.
     *
     * @param groupId group ID (used to load Jira config)
     * @param boardId the explicit Jira board ID
     * @param state   optional state filter (e.g. "active", "future",
     *                "active,future");
     *                defaults to "active,future" when null/blank
     * @return list of sprints (id + name)
     */
    List<JiraSprintDto> listSprintsByBoard(Long groupId, Long boardId, String state);
}
