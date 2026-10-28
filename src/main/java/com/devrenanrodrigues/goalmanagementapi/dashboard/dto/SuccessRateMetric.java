package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

public record SuccessRateMetric(
    int currentRate,
    int differenceFromLastMonth
) {}
