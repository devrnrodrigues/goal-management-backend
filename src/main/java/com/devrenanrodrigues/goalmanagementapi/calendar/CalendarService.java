package com.devrenanrodrigues.goalmanagementapi.calendar;

import com.devrenanrodrigues.goalmanagementapi.calendar.dto.CalendarTaskAuthorResponse;
import com.devrenanrodrigues.goalmanagementapi.calendar.dto.CalendarTaskResponse;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskRepository;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<CalendarTaskResponse> getCalendarTasks(UUID userId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : today.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();

        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TaskEntity> tasks = taskRepository.findCalendarTasks(userId, startDate, endDate, startInstant, endInstant);

        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    private CalendarTaskResponse toResponse(TaskEntity task) {
        LocalDate date = task.getDeadline() != null
                ? task.getDeadline()
                : toLocalDate(task.getCreatedAt());

        String dateKey = date.toString();
        String formattedDate = String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());

        String priority = switch (task.getPriority()) {
            case HIGH -> "Alta";
            case MEDIUM -> "Média";
            case LOW -> "Baixa";
        };

        String status = "pending";
        if (Boolean.TRUE.equals(task.getCompleted())) {
            status = "completed";
        } else if (task.getColumn() != null && task.getColumn().getTitle().toLowerCase().contains("progresso")) {
            status = "in_progress";
        }

        UserEntity firstMember = task.getAssignedMembers().stream().findFirst().orElse(null);
        CalendarTaskAuthorResponse author = buildAuthor(firstMember);

        return new CalendarTaskResponse(
                task.getId(),
                task.getTitle(),
                dateKey,
                formattedDate,
                task.getGoal().getTitle(),
                priority,
                status,
                task.getColumn() != null ? task.getColumn().getTitle() : "A Fazer",
                author
        );
    }

    private CalendarTaskAuthorResponse buildAuthor(UserEntity user) {
        if (user == null) {
            return new CalendarTaskAuthorResponse(
                    null,
                    "Membro",
                    "M",
                    null
            );
        }

        return new CalendarTaskAuthorResponse(
                user.getId(),
                user.getName(),
                getInitials(user.getName()),
                user.getAvatarUrl()
        );
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
