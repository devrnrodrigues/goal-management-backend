package com.devrenanrodrigues.goalmanagementapi.board;

import com.devrenanrodrigues.goalmanagementapi.board.dto.BoardColumnResponse;
import com.devrenanrodrigues.goalmanagementapi.board.dto.CreateBoardColumnRequest;
import com.devrenanrodrigues.goalmanagementapi.board.dto.UpdateBoardColumnRequest;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalRepository;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalSecurityValidator;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardColumnService {

    private final BoardColumnRepository boardColumnRepository;
    private final GoalRepository goalRepository;
    private final GoalSecurityValidator goalSecurityValidator;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<BoardColumnResponse> listColumnsByGoal(UUID goalId, UUID currentUserId) {
        goalSecurityValidator.validateAccess(goalId, currentUserId);
        return boardColumnRepository.findByGoalIdOrderByPositionAsc(goalId)
                .stream()
                .map(BoardColumnResponse::fromEntity)
                .toList();
    }

    @Transactional
    public BoardColumnResponse createColumn(UUID goalId, CreateBoardColumnRequest request, UUID currentUserId) {
        goalSecurityValidator.validateAdminOrOwner(goalId, currentUserId);

        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));

        int nextPosition = boardColumnRepository.countByGoalId(goalId);

        BoardColumnEntity column = BoardColumnEntity.builder()
                .goal(goal)
                .title(request.title().trim())
                .color(request.color().trim())
                .position(nextPosition)
                .build();

        BoardColumnEntity saved = boardColumnRepository.save(column);
        return BoardColumnResponse.fromEntity(saved);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID columnId, UpdateBoardColumnRequest request, UUID currentUserId) {
        BoardColumnEntity column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna não encontrada."));

        goalSecurityValidator.validateAdminOrOwner(column.getGoal().getId(), currentUserId);

        column.setTitle(request.title().trim());
        column.setColor(request.color().trim());

        BoardColumnEntity updated = boardColumnRepository.save(column);
        return BoardColumnResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteColumn(UUID columnId, UUID currentUserId) {
        BoardColumnEntity column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna não encontrada."));

        UUID goalId = column.getGoal().getId();
        goalSecurityValidator.validateAdminOrOwner(goalId, currentUserId);

        int totalColumns = boardColumnRepository.countByGoalId(goalId);
        if (totalColumns <= 1) {
            throw new BusinessException("Não é permitido excluir a única coluna existente no quadro.");
        }

        BoardColumnEntity fallbackColumn = boardColumnRepository.findFirstByGoalIdAndIdNotOrderByPositionAsc(goalId, columnId)
                .orElseThrow(() -> new BusinessException("Nenhuma coluna alternativa encontrada para remanejar as tarefas."));

        List<TaskEntity> tasksInColumn = taskRepository.findByColumnId(columnId);
        for (TaskEntity task : tasksInColumn) {
            task.setColumn(fallbackColumn);
        }
        taskRepository.saveAll(tasksInColumn);

        boardColumnRepository.delete(column);
    }
}
