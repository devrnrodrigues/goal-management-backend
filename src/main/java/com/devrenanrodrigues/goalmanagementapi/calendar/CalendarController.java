package com.devrenanrodrigues.goalmanagementapi.calendar;

import com.devrenanrodrigues.goalmanagementapi.calendar.dto.CalendarTaskResponse;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<List<CalendarTaskResponse>> getCalendarTasks(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<CalendarTaskResponse> tasks = calendarService.getCalendarTasks(authenticatedUser.id(), year, month);
        return ResponseEntity.ok(tasks);
    }
}
