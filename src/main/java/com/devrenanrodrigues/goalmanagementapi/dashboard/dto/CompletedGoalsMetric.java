package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

public record CompletedGoalsMetric(
    long total,
    long completedThisQuarter
) {}
