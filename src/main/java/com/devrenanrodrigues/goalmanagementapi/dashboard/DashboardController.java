package com.devrenanrodrigues.goalmanagementapi.dashboard;

import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.DashboardOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardOverviewResponse> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        DashboardOverviewResponse response = dashboardService.getOverview(authenticatedUser.id());
        return ResponseEntity.ok(response);
    }
}
