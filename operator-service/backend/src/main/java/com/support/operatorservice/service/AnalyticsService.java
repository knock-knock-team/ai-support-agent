package com.support.operatorservice.service;

import com.support.operatorservice.dto.analytics.DashboardAnalyticsDto;
import com.support.operatorservice.entity.Request;
import com.support.operatorservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    private final RequestRepository requestRepository;

    public DashboardAnalyticsDto getDashboardAnalytics(int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        OffsetDateTime periodStart = getPeriodStartOffset(safeDays);

        long total = safeCount(requestRepository.countAllRequests());
        long pending = safeCount(requestRepository.countByStatusIn(List.of(
                Request.Status.NEW,
                Request.Status.OPERATOR_REVIEW
        )));
        long approved = safeCount(requestRepository.countApprovedClosedRequests())
                + safeCount(requestRepository.countByStatusIn(List.of(Request.Status.AI_GENERATED)));
        long edited = safeCount(requestRepository.countEditedClosedRequests());

        List<RequestRepository.CategoryStatusCountProjection> grouped = requestRepository.countByCategoryAndStatus();
        Map<Request.Category, Counters> countersByCategory = buildCategoryCounters(grouped);

        List<DashboardAnalyticsDto.NameValue> byCategory = countersByCategory.entrySet().stream()
                .filter(entry -> entry.getValue().total > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardAnalyticsDto.NameValue(
                        toCategoryLabel(entry.getKey()),
                        entry.getValue().total
                ))
                .toList();

        List<DashboardAnalyticsDto.NameValue> byStatus = List.of(
                new DashboardAnalyticsDto.NameValue("Pending", pending),
                new DashboardAnalyticsDto.NameValue("Approved", approved),
                new DashboardAnalyticsDto.NameValue("Edited", edited)
        );

        List<DashboardAnalyticsDto.CategoryDetail> detailsByCategory = countersByCategory.entrySet().stream()
                .filter(entry -> entry.getValue().total > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Counters counters = entry.getValue();
                    long processed = counters.total - counters.pending;
                    double processingRate = counters.total == 0
                            ? 0.0
                            : Math.round(((double) processed / counters.total) * 1000.0) / 10.0;

                    return new DashboardAnalyticsDto.CategoryDetail(
                            toCategoryLabel(entry.getKey()),
                            counters.total,
                            counters.pending,
                            processed,
                            processingRate
                    );
                })
                .toList();

        List<DashboardAnalyticsDto.DailyPoint> timeSeries = buildTimeSeries(safeDays);
        DashboardAnalyticsDto.ProcessingTimeMetrics processingTimeMetrics = buildProcessingTimeMetrics(periodStart);

        return new DashboardAnalyticsDto(
                new DashboardAnalyticsDto.Summary(total, pending, approved, edited),
                byCategory,
                byStatus,
                timeSeries,
                detailsByCategory,
                processingTimeMetrics
        );
    }

    private List<DashboardAnalyticsDto.DailyPoint> buildTimeSeries(int days) {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(days - 1L);
        OffsetDateTime startOffset = getPeriodStartOffset(days);

        List<RequestRepository.DailyCountProjection> rows = requestRepository.countDailyFrom(startOffset);
        Map<LocalDate, Long> dailyTotals = new HashMap<>();
        for (RequestRepository.DailyCountProjection row : rows) {
            if (row.getDay() != null) {
                dailyTotals.put(row.getDay(), safeCount(row.getTotal()));
            }
        }

        List<DashboardAnalyticsDto.DailyPoint> result = new ArrayList<>();
        for (int index = 0; index < days; index++) {
            LocalDate day = startDate.plusDays(index);
            result.add(new DashboardAnalyticsDto.DailyPoint(
                    day.format(DAY_FORMATTER),
                    dailyTotals.getOrDefault(day, 0L)
            ));
        }

        return result;
    }

    private Map<Request.Category, Counters> buildCategoryCounters(
            List<RequestRepository.CategoryStatusCountProjection> grouped
    ) {
        Map<Request.Category, Counters> countersByCategory = new EnumMap<>(Request.Category.class);

        for (Request.Category category : Request.Category.values()) {
            countersByCategory.put(category, new Counters());
        }

        for (RequestRepository.CategoryStatusCountProjection row : grouped) {
            Request.Category category = row.getCategory() != null ? row.getCategory() : Request.Category.OTHER;
            Request.Status status = row.getStatus();
            long total = safeCount(row.getTotal());

            Counters counters = countersByCategory.computeIfAbsent(category, key -> new Counters());
            counters.total += total;

            if (status == Request.Status.NEW || status == Request.Status.OPERATOR_REVIEW) {
                counters.pending += total;
            }
        }

        return countersByCategory;
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private OffsetDateTime getPeriodStartOffset(int days) {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(days - 1L);
        return startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private DashboardAnalyticsDto.ProcessingTimeMetrics buildProcessingTimeMetrics(OffsetDateTime startDate) {
        List<Object[]> statsRows = requestRepository.getProcessingTimeStats(startDate);
        Object[] rawStats = statsRows.isEmpty() ? new Object[0] : statsRows.get(0);
        Object[] stats = unwrapStats(rawStats);
        
        long averageMinutes = 0;
        long minMinutes = 0;
        long maxMinutes = 0;
        long medianMinutes = 0;
        
        if (stats.length >= 4) {
            averageMinutes = toDisplayMinutes(toNumber(stats[0]));
            minMinutes = toDisplayMinutes(toNumber(stats[1]));
            maxMinutes = toDisplayMinutes(toNumber(stats[2]));
            medianMinutes = toDisplayMinutes(toNumber(stats[3]));
        }
        
        List<DashboardAnalyticsDto.ProcessingSpeedBucket> speedDistribution = buildSpeedDistribution(startDate);
        
        return new DashboardAnalyticsDto.ProcessingTimeMetrics(
                averageMinutes,
                minMinutes,
                maxMinutes,
                medianMinutes,
                speedDistribution
        );
    }

    private Object[] unwrapStats(Object[] rawStats) {
        if (rawStats == null) {
            return new Object[0];
        }

        if (rawStats.length == 1 && rawStats[0] instanceof Object[] nested) {
            return nested;
        }

        return rawStats;
    }

    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return null;
    }

    private long toDisplayMinutes(Number value) {
        if (value == null) {
            return 0L;
        }

        double minutes = value.doubleValue();
        if (minutes <= 0) {
            return 0L;
        }

        long rounded = Math.round(minutes);
        return rounded > 0 ? rounded : 1L;
    }

    private List<DashboardAnalyticsDto.ProcessingSpeedBucket> buildSpeedDistribution(OffsetDateTime startDate) {
        List<Object[]> times = requestRepository.getProcessingTimesForClosedRequests(startDate);
        
        long lessThanHour = 0;
        long oneToFourHours = 0;
        long fourToTwentyFourHours = 0;
        long moreThanDay = 0;
        
        for (Object[] row : times) {
            if (row == null || row.length < 4) {
                continue;
            }

            Number minutesValue = toNumber(row[3]);
            if (minutesValue == null) {
                continue;
            }

            double minutes = minutesValue.doubleValue();
            if (minutes < 60) {
                lessThanHour++;
            } else if (minutes < 240) {
                oneToFourHours++;
            } else if (minutes < 1440) {
                fourToTwentyFourHours++;
            } else {
                moreThanDay++;
            }
        }
        
        return List.of(
                new DashboardAnalyticsDto.ProcessingSpeedBucket("< 1 часа", lessThanHour),
                new DashboardAnalyticsDto.ProcessingSpeedBucket("1-4 часа", oneToFourHours),
                new DashboardAnalyticsDto.ProcessingSpeedBucket("4-24 часа", fourToTwentyFourHours),
                new DashboardAnalyticsDto.ProcessingSpeedBucket("> 1 дня", moreThanDay)
        );
    }

    private String toCategoryLabel(Request.Category category) {
        return switch (category) {
            case TECHNICAL -> "Technical";
            case BILLING -> "Billing";
            case ACCOUNT -> "Account";
            case GENERAL -> "General";
            case OTHER -> "Other";
        };
    }

    private static class Counters {
        private long total;
        private long pending;
    }
}