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
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import com.devrenanrodrigues.goalmanagementapi.task.dto.CreateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.MoveTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.TaskResponse;
import com.devrenanrodrigues.goalmanagementapi.task.dto.UpdateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalSecurityValidator goalSecurityValidator;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private TaskService taskService;

    private UUID userId;
    private UUID goalId;
    private UUID taskId;
    private UUID columnId;

    private UserEntity userEntity;
    private GoalEntity goalEntity;
    private GoalMemberEntity memberEntity;
    private BoardColumnEntity columnEntity;
    private TaskEntity taskEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        columnId = UUID.randomUUID();

        userEntity = UserEntity.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        userEntity.setId(userId);

        goalEntity = GoalEntity.builder()
                .title("Goal Project")
                .progress(0)
                .build();
        goalEntity.setId(goalId);

        memberEntity = GoalMemberEntity.builder()
                .goal(goalEntity)
                .user(userEntity)
                .role(GoalRole.OWNER)
                .build();

        columnEntity = BoardColumnEntity.builder()
                .title("A Fazer")
                .color("#3b82f6")
                .position(0)
                .goal(goalEntity)
                .build();
        columnEntity.setId(columnId);

        taskEntity = TaskEntity.builder()
                .title("Initial Task")
                .goal(goalEntity)
                .column(columnEntity)
                .completed(false)
                .priority(TaskPriority.MEDIUM)
                .position(0)
                .attachmentsCount(0)
                .commentsCount(0)
                .assignedMembers(new HashSet<>())
                .build();
        taskEntity.setId(taskId);
        taskEntity.setCreatedAt(Instant.now());
    }

    @Test
    void shouldCreateTaskSuccessfullyWithSpecifiedColumn() {
        CreateTaskRequest request = new CreateTaskRequest(
                columnId,
                "Write Unit Tests",
                "Coverage for tasks",
                TaskPriority.HIGH,
                LocalDate.now().plusDays(5),
                Collections.emptySet()
        );

        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(boardColumnRepository.findById(columnId)).thenReturn(Optional.of(columnEntity));
        when(taskRepository.countByGoalIdAndColumnId(goalId, columnId)).thenReturn(1);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            return t;
        });

        TaskResponse response = taskService.createTask(goalId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Write Unit Tests");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        verify(activityService).recordActivity(eq(userEntity), eq(goalEntity), any(), eq(ActivityActionType.TASK_CREATED), eq("Write Unit Tests"), any(), eq("A Fazer"));
    }

    @Test
    void shouldCreateTaskWithFirstColumnWhenColumnIdNotSpecified() {
        CreateTaskRequest request = new CreateTaskRequest(
                null,
                "Auto Column Task",
                null,
                null,
                null,
                null
        );

        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(boardColumnRepository.findByGoalIdOrderByPositionAsc(goalId)).thenReturn(List.of(columnEntity));
        when(taskRepository.countByGoalIdAndColumnId(goalId, columnId)).thenReturn(0);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            return t;
        });

        TaskResponse response = taskService.createTask(goalId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        verify(taskRepository).save(any(TaskEntity.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenColumnBelongsToAnotherGoalOnCreateTask() {
        GoalEntity anotherGoal = GoalEntity.builder().title("Other Goal").build();
        anotherGoal.setId(UUID.randomUUID());

        BoardColumnEntity foreignColumn = BoardColumnEntity.builder()
                .title("Foreign")
                .goal(anotherGoal)
                .build();
        foreignColumn.setId(UUID.randomUUID());

        CreateTaskRequest request = new CreateTaskRequest(foreignColumn.getId(), "Task", null, null, null, null);

        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(boardColumnRepository.findById(foreignColumn.getId())).thenReturn(Optional.of(foreignColumn));

        assertThatThrownBy(() -> taskService.createTask(goalId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A coluna informada não pertence à meta selecionada.");
    }

    @Test
    void shouldListTasksByGoalSuccessfully() {
        when(taskRepository.findByGoalIdOrderByPositionAsc(goalId)).thenReturn(List.of(taskEntity));

        List<TaskResponse> result = taskService.listTasksByGoal(goalId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Initial Task");
        verify(goalSecurityValidator).validateAccess(goalId, userId);
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Task Title",
                "New Description",
                TaskPriority.LOW,
                LocalDate.now().plusDays(10),
                null,
                null
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskEntity));
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(taskId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Updated Task Title");
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
        verify(activityService).recordActivity(eq(userEntity), eq(goalEntity), any(), eq(ActivityActionType.TASK_UPDATED), eq("Updated Task Title"), any(), any());
    }

    @Test
    void shouldMoveTaskToAnotherColumnSuccessfully() {
        UUID doneColumnId = UUID.randomUUID();
        BoardColumnEntity doneColumn = BoardColumnEntity.builder()
                .title("Concluído")
                .goal(goalEntity)
                .build();
        doneColumn.setId(doneColumnId);

        MoveTaskRequest request = new MoveTaskRequest(doneColumnId, 2);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskEntity));
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(boardColumnRepository.findById(doneColumnId)).thenReturn(Optional.of(doneColumn));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.moveTask(taskId, request, userId);

        assertThat(response).isNotNull();
        assertThat(taskEntity.getColumn()).isEqualTo(doneColumn);
        assertThat(taskEntity.getCompleted()).isTrue();
        assertThat(taskEntity.getPosition()).isEqualTo(2);
        verify(activityService).recordActivity(eq(userEntity), eq(goalEntity), eq(taskEntity), eq(ActivityActionType.TASK_MOVED), eq("Initial Task"), eq("A Fazer"), eq("Concluído"));
    }

    @Test
    void shouldToggleTaskCompleteSuccessfully() {
        BoardColumnEntity doneColumn = BoardColumnEntity.builder()
                .title("Concluído")
                .goal(goalEntity)
                .build();
        doneColumn.setId(UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskEntity));
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(boardColumnRepository.findByGoalIdOrderByPositionAsc(goalId)).thenReturn(List.of(columnEntity, doneColumn));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.toggleTaskComplete(taskId, userId);

        assertThat(response).isNotNull();
        assertThat(taskEntity.getCompleted()).isTrue();
        assertThat(taskEntity.getColumn()).isEqualTo(doneColumn);
        verify(activityService).recordActivity(eq(userEntity), eq(goalEntity), eq(taskEntity), eq(ActivityActionType.TASK_COMPLETED), eq("Initial Task"), any(), any());
    }

    @Test
    void shouldDeleteTaskSuccessfully() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskEntity));
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);

        taskService.deleteTask(taskId, userId);

        verify(taskRepository).delete(taskEntity);
        verify(activityService).recordActivity(eq(userEntity), eq(goalEntity), any(), eq(ActivityActionType.TASK_DELETED), eq("Initial Task"), any(), any());
    }
}
