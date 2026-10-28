package com.devrenanrodrigues.goalmanagementapi.report.dto;

public record PriorityEfficiencyResponse(
    String priority,
    String label,
    int completedTasks,
    int activeTasks,
    int onTimePercentage
) {}
