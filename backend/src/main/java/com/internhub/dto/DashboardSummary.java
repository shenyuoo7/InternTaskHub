package com.internhub.dto;

import java.util.Map;

public record DashboardSummary(
        long total,
        long todo,
        long inProgress,
        long done,
        long overdue,
        long dueSoon,
        double completionRate,
        Map<String, Long> byPriority) {
}
