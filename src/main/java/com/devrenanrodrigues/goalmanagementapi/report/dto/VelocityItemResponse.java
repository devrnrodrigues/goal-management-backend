package com.devrenanrodrigues.goalmanagementapi.report.dto;

public record VelocityItemResponse(
    String id,
    String monthLabel,
    int completedTasks,
    int createdTasks
) {}
