package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.goal.GoalMemberEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;

import java.time.Instant;
import java.util.UUID;

public record GoalMemberResponse(
    UUID id,
    UUID userId,
    String name,
    String email,
    String avatarUrl,
    GoalRole role,
    Instant joinedAt
) {
    public static GoalMemberResponse fromEntity(GoalMemberEntity entity) {
        return new GoalMemberResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getName(),
                entity.getUser().getEmail(),
                entity.getUser().getAvatarUrl(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }
}
