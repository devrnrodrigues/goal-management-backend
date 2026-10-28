package com.devrenanrodrigues.goalmanagementapi.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingDeadlineResponse(
    UUID id,
    String title,
    String category,
    LocalDate deadline,
    int day,
    int month,
    int year,
    String priority
) {}
