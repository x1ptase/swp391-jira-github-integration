package com.swp391.backend.dto.response;

/**
 * Projection aggregate story count theo status cho progress summary.
 * Trả về statusId, statusCode, count để build StoryProgressSummaryResponse.
 */
public interface StoryStatusCountProjection {
    Integer getStatusId();

    String getStatusCode();

    long getCount();
}
