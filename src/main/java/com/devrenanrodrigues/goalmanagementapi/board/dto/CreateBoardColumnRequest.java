package com.devrenanrodrigues.goalmanagementapi.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardColumnRequest(
    @NotBlank(message = "O título do quadro/coluna é obrigatório.")
    @Size(max = 80, message = "O título deve ter no máximo 80 caracteres.")
    String title,

    @NotBlank(message = "A cor do quadro/coluna é obrigatória.")
    String color
) {}
