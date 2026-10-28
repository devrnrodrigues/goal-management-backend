package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

public record MonthlyEvolutionResponse(
    String month,
    int planned,
    int completed
) {}
