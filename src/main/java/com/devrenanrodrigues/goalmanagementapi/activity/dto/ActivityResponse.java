package com.devrenanrodrigues.goalmanagementapi.activity.dto;

import com.devrenanrodrigues.goalmanagementapi.activity.ActivityEntity;
import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
    UUID id,
    ActivityActionType actionType,
    String title,
    UUID authorId,
    String authorName,
    String authorAvatarUrl,
    UUID goalId,
    String goalTitle,
    UUID taskId,
    String fromColumn,
    String toColumn,
    Instant createdAt
) {
    public static ActivityResponse fromEntity(ActivityEntity entity) {
        return new ActivityResponse(
                entity.getId(),
                entity.getActionType(),
                entity.getTitle(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getAuthor().getAvatarUrl(),
                entity.getGoal() != null ? entity.getGoal().getId() : null,
                entity.getGoal() != null ? entity.getGoal().getTitle() : null,
                entity.getTask() != null ? entity.getTask().getId() : null,
                entity.getFromColumn(),
                entity.getToColumn(),
                entity.getCreatedAt()
        );
    }
}
