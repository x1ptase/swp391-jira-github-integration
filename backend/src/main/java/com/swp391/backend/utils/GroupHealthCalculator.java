package com.swp391.backend.utils;

import com.swp391.backend.entity.monitoring.HealthStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupHealthCalculator {

    private static final int OVERDUE_CRITICAL = 5;
    private static final int OVERDUE_WARNING  = 3;
    private static final int COMMIT_CRITICAL  = 0;
    private static final int COMMIT_WARNING   = 2;

    private GroupHealthCalculator() {}

    public static HealthStatus calculate(long commits, long overdueTasks) {
        if (commits <= COMMIT_CRITICAL || overdueTasks >= OVERDUE_CRITICAL) {
            return HealthStatus.CRITICAL;
        }
        if (commits <= COMMIT_WARNING || overdueTasks >= OVERDUE_WARNING) {
            return HealthStatus.WARNING;
        }
        return HealthStatus.HEALTHY;
    }

    public static List<String> reasons(long commits, long overdueTasks,
                                       LocalDateTime lastActivityAt) {
        List<String> reasons = new ArrayList<>();
        boolean noActivityThisWeek = (lastActivityAt == null)
                || lastActivityAt.isBefore(LocalDateTime.now().minusDays(7));
        if (noActivityThisWeek) reasons.add("NO_ACTIVITY_THIS_WEEK");
        if (overdueTasks >= OVERDUE_WARNING) reasons.add("TOO_MANY_OVERDUE_TASKS");
        if (commits <= COMMIT_CRITICAL) reasons.add("NO_COMMITS");
        return reasons;
    }
}