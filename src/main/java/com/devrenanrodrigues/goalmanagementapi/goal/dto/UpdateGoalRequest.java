package com.devrenanrodrigues.goalmanagementapi.goal.dto;

import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalPriority;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateGoalRequest(
    @NotBlank(message = "O título da meta é obrigatório.")
    @Size(max = 180, message = "O título deve conter no máximo 180 caracteres.")
    String title,

    String description,

    @NotBlank(message = "A categoria da meta é obrigatória.")
    @Size(max = 100, message = "A categoria deve conter no máximo 100 caracteres.")
    String category,

    @NotNull(message = "O status da meta é obrigatório.")
    GoalStatus status,

    @NotNull(message = "A prioridade da meta é obrigatória.")
    GoalPriority priority,

    LocalDate deadline
) {}
