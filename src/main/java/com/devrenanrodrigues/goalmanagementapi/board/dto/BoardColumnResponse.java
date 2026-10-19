package com.devrenanrodrigues.goalmanagementapi.board.dto;

import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnEntity;

import java.util.UUID;

public record BoardColumnResponse(
    UUID id,
    UUID goalId,
    String title,
    String color,
    Integer position
) {
    public static BoardColumnResponse fromEntity(BoardColumnEntity entity) {
        return new BoardColumnResponse(
                entity.getId(),
                entity.getGoal().getId(),
                entity.getTitle(),
                entity.getColor(),
                entity.getPosition()
        );
    }
}
