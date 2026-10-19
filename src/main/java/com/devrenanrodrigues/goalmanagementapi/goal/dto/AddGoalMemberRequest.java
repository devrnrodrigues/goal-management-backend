package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddGoalMemberRequest(
    @NotNull(message = "O identificador do usuário é obrigatório.")
    UUID userId,

    @NotNull(message = "O papel do membro é obrigatório.")
    GoalRole role
) {}
