package com.devrenanrodrigues.goalmanagementapi.task.dto;

import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID goalId,
    UUID columnId,
    String columnTitle,
    String title,
    String description,
    Boolean completed,
    TaskPriority priority,
    LocalDate deadline,
    Integer position,
    Integer attachmentsCount,
    Integer commentsCount,
    List<TaskMemberSummary> members,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse fromEntity(TaskEntity entity) {
        return new TaskResponse(
                entity.getId(),
                entity.getGoal().getId(),
                entity.getColumn().getId(),
                entity.getColumn().getTitle(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCompleted(),
                entity.getPriority(),
                entity.getDeadline(),
                entity.getPosition(),
                entity.getAttachmentsCount(),
                entity.getCommentsCount(),
                entity.getAssignedMembers().stream().map(TaskMemberSummary::fromEntity).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
