package com.swp391.backend.service;

import com.swp391.backend.dto.response.JiraBoardDto;

import java.util.List;

/**
 * Service to list Jira boards for a group's configured project.
 */
public interface JiraBoardService {

    /**
     * Returns all Jira boards for the project linked to the given group.
     *
     * @param groupId group ID
     * @return list of boards (id + name)
     */
    List<JiraBoardDto> listBoards(Long groupId);
}
