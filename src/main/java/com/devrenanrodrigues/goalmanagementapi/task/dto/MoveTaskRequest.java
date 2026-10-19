package com.devrenanrodrigues.goalmanagementapi.task.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveTaskRequest(
    @NotNull(message = "A coluna de destino é obrigatória.")
    UUID targetColumnId,

    Integer targetPosition
) {}
