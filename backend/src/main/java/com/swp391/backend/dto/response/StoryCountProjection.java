package com.swp391.backend.dto.response;

/**
 * Projection dùng để aggregate story count theo requirementId từ
 * TaskRepository.
 * Tránh N+1 khi build dashboard response.
 */
public interface StoryCountProjection {
    Integer getRequirementId();

    long getCount();
}
