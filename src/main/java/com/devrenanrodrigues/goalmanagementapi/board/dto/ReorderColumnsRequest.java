package com.devrenanrodrigues.goalmanagementapi.board.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderColumnsRequest(
        @NotEmpty(message = "A lista de colunas não pode ser vazia.")
        List<UUID> columnIds
) {}
