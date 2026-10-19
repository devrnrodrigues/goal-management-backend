package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalPriority;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalSummaryResponse(
    UUID id,
    String title,
    String description,
    String category,
    GoalStatus status,
    GoalPriority priority,
    LocalDate deadline,
    Integer progress,
    int memberCount,
    GoalRole userRole,
    Instant createdAt
) {
    public static GoalSummaryResponse of(GoalEntity entity, GoalRole userRole) {
        return new GoalSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getDeadline(),
                entity.getProgress(),
                entity.getMembers().size(),
                userRole,
                entity.getCreatedAt()
        );
    }
}
