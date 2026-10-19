package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.AddGoalMemberRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.CreateGoalRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalMemberResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalSummaryResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.UpdateGoalRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @Valid @RequestBody CreateGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GoalResponse response = goalService.createGoal(request, authenticatedUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<GoalSummaryResponse>> listGoals(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable
    ) {
        Page<GoalSummaryResponse> response = goalService.listUserGoals(authenticatedUser.id(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GoalResponse response = goalService.getGoalById(id, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GoalResponse response = goalService.updateGoal(id, request, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        goalService.deleteGoal(id, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GoalMemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddGoalMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GoalMemberResponse response = goalService.addMember(id, request, authenticatedUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GoalMemberResponse>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<GoalMemberResponse> response = goalService.listGoalMembers(id, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        goalService.removeMember(id, userId, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
