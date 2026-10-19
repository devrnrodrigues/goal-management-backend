package com.devrenanrodrigues.goalmanagementapi.task.dto;

import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateTaskRequest(
    UUID columnId,

    @NotBlank(message = "O título da tarefa é obrigatório.")
    @Size(max = 200, message = "O título deve conter no máximo 200 caracteres.")
    String title,

    String description,
    TaskPriority priority,
    LocalDate deadline,
    Set<UUID> assignedMemberIds
) {}
