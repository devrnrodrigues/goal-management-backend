package com.devrenanrodrigues.goalmanagementapi.task;

import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.task.dto.CreateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.MoveTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.TaskResponse;
import com.devrenanrodrigues.goalmanagementapi.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/api/goals/{goalId}/tasks")
    public ResponseEntity<List<TaskResponse>> listTasks(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<TaskResponse> response = taskService.listTasksByGoal(goalId, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/goals/{goalId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TaskResponse response = taskService.createTask(goalId, request, authenticatedUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TaskResponse response = taskService.updateTask(taskId, request, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/tasks/{taskId}/move")
    public ResponseEntity<TaskResponse> moveTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TaskResponse response = taskService.moveTask(taskId, request, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/tasks/{taskId}/toggle-complete")
    public ResponseEntity<TaskResponse> toggleTaskComplete(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TaskResponse response = taskService.toggleTaskComplete(taskId, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        taskService.deleteTask(taskId, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
