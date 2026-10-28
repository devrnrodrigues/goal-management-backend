package com.devrenanrodrigues.goalmanagementapi.report;

import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ReportResponse> getReport(
            @RequestParam(name = "period", required = false, defaultValue = "30_days") String period,
            @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ReportResponse response = reportService.getReport(authenticatedUser.id(), period, category);
        return ResponseEntity.ok(response);
    }
}
