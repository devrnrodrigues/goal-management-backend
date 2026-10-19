package com.devrenanrodrigues.goalmanagementapi.task;

import com.devrenanrodrigues.goalmanagementapi.activity.ActivityService;
import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;
import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnEntity;
import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnRepository;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalMemberEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalRepository;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalSecurityValidator;
import com.devrenanrodrigues.goalmanagementapi.task.dto.CreateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.MoveTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.TaskResponse;
import com.devrenanrodrigues.goalmanagementapi.task.dto.UpdateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final GoalSecurityValidator goalSecurityValidator;
    private final ActivityService activityService;

    @Transactional
    public TaskResponse createTask(UUID goalId, CreateTaskRequest request, UUID currentUserId) {
        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(goalId, currentUserId);

        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));

        BoardColumnEntity column;
        if (request.columnId() != null) {
            column = boardColumnRepository.findById(request.columnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Coluna não encontrada."));
            if (!column.getGoal().getId().equals(goalId)) {
                throw new BusinessException("A coluna informada não pertence à meta selecionada.");
            }
        } else {
            List<BoardColumnEntity> columns = boardColumnRepository.findByGoalIdOrderByPositionAsc(goalId);
            if (columns.isEmpty()) {
                throw new BusinessException("A meta não possui colunas para receber a tarefa.");
            }
            column = columns.getFirst();
        }

        int position = taskRepository.countByGoalIdAndColumnId(goalId, column.getId());

        Set<UserEntity> assignedMembers = new HashSet<>();
        if (request.assignedMemberIds() != null && !request.assignedMemberIds().isEmpty()) {
            assignedMembers.addAll(userRepository.findAllById(request.assignedMemberIds()));
        }

        TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;

        TaskEntity task = TaskEntity.builder()
                .goal(goal)
                .column(column)
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .completed(false)
                .priority(priority)
                .deadline(request.deadline())
                .position(position)
                .attachmentsCount(0)
                .commentsCount(0)
                .assignedMembers(assignedMembers)
                .build();

        TaskEntity savedTask = taskRepository.save(task);
        updateGoalProgress(goal);

        activityService.recordActivity(
                member.getUser(),
                goal,
                savedTask,
                ActivityActionType.TASK_CREATED,
                "Criou a tarefa " + savedTask.getTitle(),
                null,
                column.getTitle()
        );

        return TaskResponse.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksByGoal(UUID goalId, UUID currentUserId) {
        goalSecurityValidator.validateAccess(goalId, currentUserId);
        return taskRepository.findByGoalIdOrderByPositionAsc(goalId)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UUID currentUserId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));

        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(task.getGoal().getId(), currentUserId);

        task.setTitle(request.title().trim());
        task.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDeadline(request.deadline());

        if (request.columnId() != null && !task.getColumn().getId().equals(request.columnId())) {
            BoardColumnEntity newColumn = boardColumnRepository.findById(request.columnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Coluna não encontrada."));
            if (!newColumn.getGoal().getId().equals(task.getGoal().getId())) {
                throw new BusinessException("A coluna informada não pertence à mesma meta.");
            }
            task.setColumn(newColumn);
            if (isDoneColumn(newColumn)) {
                task.setCompleted(true);
            }
        }

        if (request.assignedMemberIds() != null) {
            Set<UserEntity> updatedMembers = new HashSet<>(userRepository.findAllById(request.assignedMemberIds()));
            task.setAssignedMembers(updatedMembers);
        }

        TaskEntity updatedTask = taskRepository.save(task);
        updateGoalProgress(task.getGoal());

        activityService.recordActivity(
                member.getUser(),
                task.getGoal(),
                updatedTask,
                ActivityActionType.TASK_UPDATED,
                "Atualizou a tarefa " + updatedTask.getTitle(),
                null,
                null
        );

        return TaskResponse.fromEntity(updatedTask);
    }

    @Transactional
    public TaskResponse moveTask(UUID taskId, MoveTaskRequest request, UUID currentUserId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));

        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(task.getGoal().getId(), currentUserId);

        BoardColumnEntity targetColumn = boardColumnRepository.findById(request.targetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Coluna de destino não encontrada."));

        if (!targetColumn.getGoal().getId().equals(task.getGoal().getId())) {
            throw new BusinessException("Não é permitido mover tarefas para colunas de outras metas.");
        }

        String originColumnTitle = task.getColumn().getTitle();
        task.setColumn(targetColumn);

        if (isDoneColumn(targetColumn)) {
            task.setCompleted(true);
        }

        if (request.targetPosition() != null) {
            task.setPosition(request.targetPosition());
        }

        TaskEntity saved = taskRepository.save(task);
        updateGoalProgress(task.getGoal());

        activityService.recordActivity(
                member.getUser(),
                task.getGoal(),
                saved,
                ActivityActionType.TASK_MOVED,
                "Moveu a tarefa " + saved.getTitle() + " de " + originColumnTitle + " para " + targetColumn.getTitle(),
                originColumnTitle,
                targetColumn.getTitle()
        );

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse toggleTaskComplete(UUID taskId, UUID currentUserId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));

        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(task.getGoal().getId(), currentUserId);

        boolean nextCompleted = !task.getCompleted();
        task.setCompleted(nextCompleted);

        List<BoardColumnEntity> columns = boardColumnRepository.findByGoalIdOrderByPositionAsc(task.getGoal().getId());
        if (!columns.isEmpty()) {
            if (nextCompleted) {
                columns.stream().filter(this::isDoneColumn).findFirst().ifPresent(task::setColumn);
            } else if (isDoneColumn(task.getColumn())) {
                task.setColumn(columns.getFirst());
            }
        }

        TaskEntity saved = taskRepository.save(task);
        updateGoalProgress(task.getGoal());

        activityService.recordActivity(
                member.getUser(),
                task.getGoal(),
                saved,
                nextCompleted ? ActivityActionType.TASK_COMPLETED : ActivityActionType.TASK_UPDATED,
                nextCompleted ? "Concluiu a tarefa " + saved.getTitle() : "Reabriu a tarefa " + saved.getTitle(),
                null,
                null
        );

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID currentUserId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada."));

        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(task.getGoal().getId(), currentUserId);
        GoalEntity goal = task.getGoal();

        taskRepository.delete(task);
        updateGoalProgress(goal);

        activityService.recordActivity(
                member.getUser(),
                goal,
                null,
                ActivityActionType.TASK_DELETED,
                "Excluiu a tarefa " + task.getTitle(),
                null,
                null
        );
    }

    private void updateGoalProgress(GoalEntity goal) {
        int total = taskRepository.countByGoalId(goal.getId());
        int completed = taskRepository.countByGoalIdAndCompletedTrue(goal.getId());
        int progress = total > 0 ? (int) Math.round(((double) completed / total) * 100) : 0;
        goal.setProgress(progress);
        goalRepository.save(goal);
    }

    private boolean isDoneColumn(BoardColumnEntity column) {
        return column.getTitle().toLowerCase().contains("conclu") || column.getTitle().toLowerCase().contains("done");
    }
}
