package com.devrenanrodrigues.goalmanagementapi.report.dto;

public record CategoryDistributionResponse(
    String id,
    String category,
    int goalsCount,
    int percentage,
    String color
) {}
