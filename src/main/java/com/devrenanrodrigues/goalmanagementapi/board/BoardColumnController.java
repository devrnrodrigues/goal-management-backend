package com.devrenanrodrigues.goalmanagementapi.board;

import com.devrenanrodrigues.goalmanagementapi.board.dto.BoardColumnResponse;
import com.devrenanrodrigues.goalmanagementapi.board.dto.CreateBoardColumnRequest;
import com.devrenanrodrigues.goalmanagementapi.board.dto.UpdateBoardColumnRequest;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BoardColumnController {

    private final BoardColumnService boardColumnService;

    @GetMapping("/api/goals/{goalId}/columns")
    public ResponseEntity<List<BoardColumnResponse>> listColumns(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<BoardColumnResponse> response = boardColumnService.listColumnsByGoal(goalId, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/goals/{goalId}/columns")
    public ResponseEntity<BoardColumnResponse> createColumn(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateBoardColumnRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        BoardColumnResponse response = boardColumnService.createColumn(goalId, request, authenticatedUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/columns/{columnId}")
    public ResponseEntity<BoardColumnResponse> updateColumn(
            @PathVariable UUID columnId,
            @Valid @RequestBody UpdateBoardColumnRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        BoardColumnResponse response = boardColumnService.updateColumn(columnId, request, authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable UUID columnId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boardColumnService.deleteColumn(columnId, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
