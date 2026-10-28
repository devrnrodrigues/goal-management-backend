package com.devrenanrodrigues.goalmanagementapi.calendar.dto;

import java.util.UUID;

public record CalendarTaskAuthorResponse(
    UUID id,
    String name,
    String initials,
    String avatarUrl
) {}
