package com.swp391.backend.utils;

import com.swp391.backend.entity.ContributionStatus;

public class StudentStatusCalculator {

    private static final int LOW_THRESHOLD = 2;

    private StudentStatusCalculator() {}

    public static ContributionStatus calculate(long commitCount) {
        if (commitCount == 0) return ContributionStatus.NO_CONTRIBUTION;
        if (commitCount <= LOW_THRESHOLD) return ContributionStatus.LOW;
        return ContributionStatus.NORMAL;
    }

    public static boolean isFlagged(long commitCount) {
        ContributionStatus status = calculate(commitCount);
        return status == ContributionStatus.LOW
                || status == ContributionStatus.NO_CONTRIBUTION;
    }
}