package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.board.dto.BoardColumnResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalPriority;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalResponse(
    UUID id,
    String title,
    String description,
    String category,
    GoalStatus status,
    GoalPriority priority,
    LocalDate deadline,
    Integer progress,
    GoalRole userRole,
    List<GoalMemberResponse> members,
    List<BoardColumnResponse> columns,
    Instant createdAt,
    Instant updatedAt
) {
    public static GoalResponse of(GoalEntity entity, GoalRole userRole) {
        return new GoalResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getDeadline(),
                entity.getProgress(),
                userRole,
                entity.getMembers().stream().map(GoalMemberResponse::fromEntity).toList(),
                entity.getColumns().stream().map(BoardColumnResponse::fromEntity).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
