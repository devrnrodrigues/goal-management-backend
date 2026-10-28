package com.devrenanrodrigues.goalmanagementapi.report.dto;

public record ReportOverviewMetric(
    String id,
    String label,
    String value,
    String change,
    boolean isPositive,
    String subtitle
) {}
