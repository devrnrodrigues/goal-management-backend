package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

import java.util.List;

public record DashboardOverviewResponse(
    DashboardMetricsResponse metrics,
    List<DashboardFocusGoalResponse> focusGoals,
    List<MonthlyEvolutionResponse> monthlyEvolution,
    List<UpcomingDeadlineResponse> upcomingDeadlines
) {}
