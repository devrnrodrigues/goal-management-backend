package com.devrenanrodrigues.goalmanagementapi.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String name,

    @Size(max = 500, message = "A URL do avatar deve ter no máximo 500 caracteres.")
    String avatarUrl,

    String theme,
    Boolean emailNotifications,
    Boolean weeklySummary,

    String currentPassword,
    String newPassword
) {}

