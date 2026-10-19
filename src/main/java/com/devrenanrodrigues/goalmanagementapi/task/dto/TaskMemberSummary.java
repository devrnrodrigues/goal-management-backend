package com.devrenanrodrigues.goalmanagementapi.task.dto;

import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;

import java.util.UUID;

public record TaskMemberSummary(
    UUID id,
    String name,
    String avatarUrl
) {
    public static TaskMemberSummary fromEntity(UserEntity user) {
        return new TaskMemberSummary(user.getId(), user.getName(), user.getAvatarUrl());
    }
}
