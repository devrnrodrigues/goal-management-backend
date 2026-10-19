package com.devrenanrodrigues.goalmanagementapi.activity;

import com.devrenanrodrigues.goalmanagementapi.activity.dto.ActivityResponse;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> listUserActivities(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable
    ) {
        Page<ActivityResponse> activities = activityService.listUserActivities(authenticatedUser.id(), pageable);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/goals/{goalId}")
    public ResponseEntity<Page<ActivityResponse>> listGoalActivities(
            @PathVariable UUID goalId,
            Pageable pageable
    ) {
        Page<ActivityResponse> activities = activityService.listGoalActivities(goalId, pageable);
        return ResponseEntity.ok(activities);
    }
}
