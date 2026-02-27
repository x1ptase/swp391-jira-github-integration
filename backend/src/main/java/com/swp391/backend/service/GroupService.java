package com.swp391.backend.service;

import java.util.List;

public interface GroupService {
    /**
     * Checks if a user is authorized for a specific group based on allowed roles.
     * 
     * @param userId       the ID of the user
     * @param groupId      the ID of the group
     * @param allowedRoles list of allowed role codes (e.g., "LEADER", "ADMIN")
     * @return true if authorized, false otherwise
     */
    boolean isUserAuthorized(Long userId, Long groupId, List<String> allowedRoles);
}
