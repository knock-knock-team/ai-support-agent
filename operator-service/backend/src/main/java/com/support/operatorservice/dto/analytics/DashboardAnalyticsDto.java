package com.support.operatorservice.dto.analytics;

import java.util.List;

public record DashboardAnalyticsDto(
        Summary summary,
        List<NameValue> byCategory,
        List<NameValue> byStatus,
        List<DailyPoint> timeSeries,
        List<CategoryDetail> detailsByCategory
) {
    public record Summary(long total, long pending, long approved, long edited) {
    }

    public record NameValue(String name, long value) {
    }

    public record DailyPoint(String date, long requests) {
    }

    public record CategoryDetail(
            String category,
            long total,
            long pending,
            long processed,
            double processingRate
    ) {
    }
}