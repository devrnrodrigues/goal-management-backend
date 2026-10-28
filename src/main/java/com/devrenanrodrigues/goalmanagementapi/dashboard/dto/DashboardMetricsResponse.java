package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

public record DashboardMetricsResponse(
    ActiveGoalsMetric activeGoals,
    SuccessRateMetric successRate,
    TodayActionsMetric todayActions,
    AttentionGoalsMetric attentionGoals,
    StreakMetric streak,
    CompletedGoalsMetric completedGoals
) {}
