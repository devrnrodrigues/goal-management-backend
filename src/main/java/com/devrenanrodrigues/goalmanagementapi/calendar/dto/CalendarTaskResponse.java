package com.devrenanrodrigues.goalmanagementapi.calendar.dto;

import java.util.UUID;

public record CalendarTaskResponse(
    UUID id,
    String title,
    String createdAt,
    String formattedDate,
    String goalTitle,
    String priority,
    String status,
    String columnName,
    CalendarTaskAuthorResponse author
) {}
