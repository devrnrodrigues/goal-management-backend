package com.devrenanrodrigues.goalmanagementapi.dashboard;

import com.devrenanrodrigues.goalmanagementapi.activity.ActivityRepository;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.ActiveGoalsMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.AttentionGoalsMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.CompletedGoalsMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.DashboardFocusGoalResponse;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.DashboardMetricsResponse;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.DashboardOverviewResponse;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.MonthlyEvolutionResponse;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.StreakMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.SuccessRateMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.TodayActionsMetric;
import com.devrenanrodrigues.goalmanagementapi.dashboard.dto.UpcomingDeadlineResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalRepository;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String[] MONTH_NAMES = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(UUID userId) {
        LocalDate today = LocalDate.now();
        List<GoalEntity> userGoals = goalRepository.findAllByMembersUserId(userId);

        DashboardMetricsResponse metrics = buildMetrics(userGoals, userId, today);
        List<DashboardFocusGoalResponse> focusGoals = buildFocusGoals(userId);
        List<MonthlyEvolutionResponse> monthlyEvolution = buildMonthlyEvolution(userGoals, today);
        List<UpcomingDeadlineResponse> upcomingDeadlines = buildUpcomingDeadlines(userGoals, today);

        return new DashboardOverviewResponse(metrics, focusGoals, monthlyEvolution, upcomingDeadlines);
    }

    private DashboardMetricsResponse buildMetrics(List<GoalEntity> userGoals, UUID userId, LocalDate today) {
        long activeGoalsCount = userGoals.stream()
                .filter(g -> g.getStatus() != GoalStatus.COMPLETED)
                .count();

        LocalDate startOfMonth = today.withDayOfMonth(1);
        long addedThisMonth = userGoals.stream()
                .filter(g -> !toLocalDate(g.getCreatedAt()).isBefore(startOfMonth))
                .count();

        ActiveGoalsMetric activeGoals = new ActiveGoalsMetric(activeGoalsCount, addedThisMonth);

        long totalGoals = userGoals.size();
        long completedGoalsCount = userGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.COMPLETED)
                .count();

        int currentRate = totalGoals > 0 ? (int) Math.round(((double) completedGoalsCount / totalGoals) * 100) : 0;

        List<GoalEntity> previousMonthGoals = userGoals.stream()
                .filter(g -> toLocalDate(g.getCreatedAt()).isBefore(startOfMonth))
                .toList();
        long prevTotal = previousMonthGoals.size();
        long prevCompleted = previousMonthGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.COMPLETED && toLocalDate(g.getUpdatedAt()).isBefore(startOfMonth))
                .count();
        int prevRate = prevTotal > 0 ? (int) Math.round(((double) prevCompleted / prevTotal) * 100) : currentRate;
        int diffFromLastMonth = currentRate - prevRate;

        SuccessRateMetric successRate = new SuccessRateMetric(currentRate, diffFromLastMonth);

        long totalToday = taskRepository.countTasksDueToday(userId, today);
        long completedToday = taskRepository.countTasksCompletedToday(userId, today);
        TodayActionsMetric todayActions = new TodayActionsMetric(completedToday, totalToday);

        LocalDate in7Days = today.plusDays(7);
        long attentionTotal = userGoals.stream()
                .filter(g -> g.getStatus() != GoalStatus.COMPLETED && g.getDeadline() != null && !g.getDeadline().isAfter(in7Days))
                .count();
        long dueNext7Days = userGoals.stream()
                .filter(g -> g.getStatus() != GoalStatus.COMPLETED && g.getDeadline() != null && !g.getDeadline().isBefore(today) && !g.getDeadline().isAfter(in7Days))
                .count();
        AttentionGoalsMetric attentionGoals = new AttentionGoalsMetric(attentionTotal, dueNext7Days);

        StreakMetric streak = calculateStreak(userId, today);

        LocalDate firstDayOfQuarter = today.with(today.getMonth().firstMonthOfQuarter()).withDayOfMonth(1);
        long completedThisQuarter = userGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.COMPLETED && !toLocalDate(g.getUpdatedAt()).isBefore(firstDayOfQuarter))
                .count();
        CompletedGoalsMetric completedGoals = new CompletedGoalsMetric(completedGoalsCount, completedThisQuarter);

        return new DashboardMetricsResponse(activeGoals, successRate, todayActions, attentionGoals, streak, completedGoals);
    }

    private StreakMetric calculateStreak(UUID userId, LocalDate today) {
        List<Instant> instants = activityRepository.findActivityInstantsByUserId(userId);
        if (instants.isEmpty()) {
            return new StreakMetric(0, 0);
        }

        List<LocalDate> dates = instants.stream()
                .map(this::toLocalDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        LocalDate mostRecent = dates.getFirst();
        int currentStreak = 0;

        if (mostRecent.equals(today) || mostRecent.equals(today.minusDays(1))) {
            LocalDate expectedDate = mostRecent;
            for (LocalDate date : dates) {
                if (date.equals(expectedDate)) {
                    currentStreak++;
                    expectedDate = expectedDate.minusDays(1);
                } else {
                    break;
                }
            }
        }

        int longestStreak = 0;
        int runningStreak = 0;
        LocalDate previousDate = null;

        for (LocalDate date : dates) {
            if (previousDate == null || previousDate.minusDays(1).equals(date)) {
                runningStreak++;
            } else {
                runningStreak = 1;
            }
            if (runningStreak > longestStreak) {
                longestStreak = runningStreak;
            }
            previousDate = date;
        }

        return new StreakMetric(currentStreak, Math.max(longestStreak, currentStreak));
    }

    private List<DashboardFocusGoalResponse> buildFocusGoals(UUID userId) {
        List<GoalEntity> focusEntities = goalRepository.findFocusGoalsByUserId(userId, PageRequest.of(0, 3));
        return focusEntities.stream()
                .map(goal -> {
                    Optional<TaskEntity> nextTask = taskRepository.findFirstByGoalIdAndCompletedFalseOrderByPositionAsc(goal.getId());
                    String currentStep = nextTask.map(TaskEntity::getTitle).orElse("Nenhuma etapa pendente");
                    return new DashboardFocusGoalResponse(
                            goal.getId(),
                            goal.getTitle(),
                            goal.getCategory(),
                            goal.getProgress(),
                            goal.getDeadline(),
                            currentStep,
                            goal.getPriority().name()
                    );
                })
                .toList();
    }

    private List<MonthlyEvolutionResponse> buildMonthlyEvolution(List<GoalEntity> userGoals, LocalDate today) {
        List<MonthlyEvolutionResponse> list = new ArrayList<>();
        int currentYear = today.getYear();

        for (int monthIndex = 1; monthIndex <= 12; monthIndex++) {
            YearMonth targetYearMonth = YearMonth.of(currentYear, monthIndex);
            LocalDate start = targetYearMonth.atDay(1);
            LocalDate end = targetYearMonth.atEndOfMonth();

            int planned = (int) userGoals.stream()
                    .filter(g -> {
                        LocalDate created = toLocalDate(g.getCreatedAt());
                        return !created.isBefore(start) && !created.isAfter(end);
                    })
                    .count();

            int completed = (int) userGoals.stream()
                    .filter(g -> {
                        if (g.getStatus() != GoalStatus.COMPLETED) {
                            return false;
                        }
                        LocalDate updated = toLocalDate(g.getUpdatedAt());
                        return !updated.isBefore(start) && !updated.isAfter(end);
                    })
                    .count();

            list.add(new MonthlyEvolutionResponse(MONTH_NAMES[monthIndex - 1], planned, completed));
        }

        return list;
    }

    private List<UpcomingDeadlineResponse> buildUpcomingDeadlines(List<GoalEntity> userGoals, LocalDate today) {
        return userGoals.stream()
                .filter(g -> g.getStatus() != GoalStatus.COMPLETED && g.getDeadline() != null && !g.getDeadline().isBefore(today))
                .sorted(Comparator.comparing(GoalEntity::getDeadline))
                .limit(10)
                .map(g -> new UpcomingDeadlineResponse(
                        g.getId(),
                        g.getTitle(),
                        g.getCategory(),
                        g.getDeadline(),
                        g.getDeadline().getDayOfMonth(),
                        g.getDeadline().getMonthValue(),
                        g.getDeadline().getYear(),
                        g.getPriority().name()
                ))
                .toList();
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
