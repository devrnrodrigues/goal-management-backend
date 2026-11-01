package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import jakarta.validation.constraints.NotNull;

public record UpdateGoalMemberRoleRequest(
    @NotNull(message = "O novo papel do membro é obrigatório.")
    GoalRole role
) {}

