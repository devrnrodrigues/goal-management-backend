package com.devrenanrodrigues.goalmanagementapi.report.dto;

import java.util.UUID;

public record GoalHealthItemResponse(
    UUID id,
    String goalTitle,
    String category,
    int progress,
    int completedTasks,
    int totalTasks,
    String deadline,
    String healthStatus
) {}
