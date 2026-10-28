package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DashboardFocusGoalResponse(
    UUID id,
    String title,
    String category,
    Integer progress,
    LocalDate deadline,
    String currentStep,
    String priority
) {}
