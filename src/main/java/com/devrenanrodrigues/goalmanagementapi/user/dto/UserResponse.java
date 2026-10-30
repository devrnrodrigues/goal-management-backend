package com.devrenanrodrigues.goalmanagementapi.user.dto;

import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String avatarUrl,
    AuthProvider provider,
    String theme,
    Boolean emailNotifications,
    Boolean weeklySummary,
    Instant createdAt
) {
    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getAvatarUrl(),
                entity.getProvider(),
                entity.getTheme() != null ? entity.getTheme() : "LIGHT",
                entity.getEmailNotifications() != null ? entity.getEmailNotifications() : true,
                entity.getWeeklySummary() != null ? entity.getWeeklySummary() : true,
                entity.getCreatedAt()
        );
    }
}
