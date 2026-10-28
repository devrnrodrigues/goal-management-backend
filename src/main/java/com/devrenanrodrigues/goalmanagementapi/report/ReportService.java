package com.devrenanrodrigues.goalmanagementapi.report;

import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalRepository;
import com.devrenanrodrigues.goalmanagementapi.report.dto.CategoryDistributionResponse;
import com.devrenanrodrigues.goalmanagementapi.report.dto.GoalHealthItemResponse;
import com.devrenanrodrigues.goalmanagementapi.report.dto.PriorityEfficiencyResponse;
import com.devrenanrodrigues.goalmanagementapi.report.dto.ReportOverviewMetric;
import com.devrenanrodrigues.goalmanagementapi.report.dto.ReportResponse;
import com.devrenanrodrigues.goalmanagementapi.report.dto.VelocityItemResponse;
import com.devrenanrodrigues.goalmanagementapi.report.enums.GoalHealthStatus;
import com.devrenanrodrigues.goalmanagementapi.report.enums.ReportPeriod;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskRepository;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String[] MONTH_NAMES = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private static final String[] CATEGORY_COLORS = {
        "#006d44", "#3b82f6", "#f59e0b", "#8b5cf6", "#ef4444", "#06b6d4", "#ec4899"
    };

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID userId, String periodStr, String category) {
        ReportPeriod period = ReportPeriod.fromString(periodStr);
        LocalDate today = LocalDate.now();

        LocalDate startDate = calculateStartDate(period, today);
        long daysDiff = Math.max(1, ChronoUnit.DAYS.between(startDate, today));
        LocalDate prevStartDate = startDate.minusDays(daysDiff);

        List<GoalEntity> allUserGoals = goalRepository.findAllByMembersUserId(userId);
        List<String> availableCategories = allUserGoals.stream()
                .map(GoalEntity::getCategory)
                .distinct()
                .sorted()
                .toList();

        List<GoalEntity> filteredGoals = allUserGoals;
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            filteredGoals = allUserGoals.stream()
                    .filter(g -> g.getCategory().equalsIgnoreCase(category.trim()))
                    .toList();
        }

        List<TaskEntity> allUserTasks = taskRepository.findAllByUserId(userId);
        List<TaskEntity> filteredTasks = allUserTasks;
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            filteredTasks = allUserTasks.stream()
                    .filter(t -> t.getGoal().getCategory().equalsIgnoreCase(category.trim()))
                    .toList();
        }

        List<ReportOverviewMetric> metrics = buildOverviewMetrics(filteredGoals, filteredTasks, startDate, prevStartDate, today);
        List<VelocityItemResponse> velocityData = buildVelocityData(filteredTasks, today);
        List<CategoryDistributionResponse> categoryDistribution = buildCategoryDistribution(filteredGoals);
        List<PriorityEfficiencyResponse> priorityEfficiencies = buildPriorityEfficiencies(filteredTasks);
        List<GoalHealthItemResponse> goalsHealth = buildGoalsHealth(filteredGoals, today);

        return new ReportResponse(
                metrics,
                velocityData,
                categoryDistribution,
                priorityEfficiencies,
                goalsHealth,
                availableCategories
        );
    }

    private LocalDate calculateStartDate(ReportPeriod period, LocalDate today) {
        return switch (period) {
            case QUARTER -> today.minusMonths(3);
            case YEAR -> today.minusYears(1);
            default -> today.minusDays(30);
        };
    }

    private List<ReportOverviewMetric> buildOverviewMetrics(
            List<GoalEntity> goals,
            List<TaskEntity> tasks,
            LocalDate startDate,
            LocalDate prevStartDate,
            LocalDate today
    ) {
        long currentTotalTasks = tasks.stream()
                .filter(t -> !toLocalDate(t.getCreatedAt()).isBefore(startDate))
                .count();
        long currentCompletedTasks = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getCreatedAt()).isBefore(startDate))
                .count();
        int currentCompletionRate = currentTotalTasks > 0
                ? (int) Math.round(((double) currentCompletedTasks / currentTotalTasks) * 100)
                : 0;

        long prevTotalTasks = tasks.stream()
                .filter(t -> !toLocalDate(t.getCreatedAt()).isBefore(prevStartDate) && toLocalDate(t.getCreatedAt()).isBefore(startDate))
                .count();
        long prevCompletedTasks = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getCreatedAt()).isBefore(prevStartDate) && toLocalDate(t.getCreatedAt()).isBefore(startDate))
                .count();
        int prevCompletionRate = prevTotalTasks > 0
                ? (int) Math.round(((double) prevCompletedTasks / prevTotalTasks) * 100)
                : currentCompletionRate;
        int rateDiff = currentCompletionRate - prevCompletionRate;

        double avgCycleDays = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getUpdatedAt()).isBefore(startDate))
                .mapToLong(t -> Math.max(0, ChronoUnit.DAYS.between(toLocalDate(t.getCreatedAt()), toLocalDate(t.getUpdatedAt()))))
                .average()
                .orElse(0.0);

        double prevAvgCycleDays = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getUpdatedAt()).isBefore(prevStartDate) && toLocalDate(t.getUpdatedAt()).isBefore(startDate))
                .mapToLong(t -> Math.max(0, ChronoUnit.DAYS.between(toLocalDate(t.getCreatedAt()), toLocalDate(t.getUpdatedAt()))))
                .average()
                .orElse(avgCycleDays);
        double cycleDiff = avgCycleDays - prevAvgCycleDays;

        long goalsWithDeadline = goals.stream()
                .filter(g -> g.getDeadline() != null)
                .count();
        long goalsOnTime = goals.stream()
                .filter(g -> g.getDeadline() != null && !today.isAfter(g.getDeadline()))
                .count();
        int onTimeGoalsRate = goalsWithDeadline > 0
                ? (int) Math.round(((double) goalsOnTime / goalsWithDeadline) * 100)
                : 100;

        long deliveredCurrent = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getUpdatedAt()).isBefore(startDate))
                .count();
        long deliveredPrev = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()) && !toLocalDate(t.getUpdatedAt()).isBefore(prevStartDate) && toLocalDate(t.getUpdatedAt()).isBefore(startDate))
                .count();
        long deliveredDiff = deliveredCurrent - deliveredPrev;
        double deliveredPercentChange = deliveredPrev > 0
                ? ((double) deliveredDiff / deliveredPrev) * 100
                : (deliveredCurrent > 0 ? 100.0 : 0.0);

        return List.of(
                new ReportOverviewMetric(
                        "m-1",
                        "Taxa de conclusão",
                        currentCompletionRate + "%",
                        (rateDiff >= 0 ? "+" : "") + rateDiff + "%",
                        rateDiff >= 0,
                        "vs. período anterior"
                ),
                new ReportOverviewMetric(
                        "m-2",
                        "Tempo médio de ciclo",
                        String.format(Locale.US, "%.1f dias", avgCycleDays),
                        String.format(Locale.US, "%s%.1f dias", cycleDiff > 0 ? "+" : "", cycleDiff),
                        cycleDiff <= 0,
                        "agilidade na entrega"
                ),
                new ReportOverviewMetric(
                        "m-3",
                        "Metas no prazo",
                        onTimeGoalsRate + "%",
                        "+0%",
                        true,
                        "aderência ao cronograma"
                ),
                new ReportOverviewMetric(
                        "m-4",
                        "Tarefas entregues",
                        String.valueOf(deliveredCurrent),
                        String.format(Locale.US, "%s%.0f%%", deliveredPercentChange >= 0 ? "+" : "", deliveredPercentChange),
                        deliveredPercentChange >= 0,
                        "neste ciclo"
                )
        );
    }

    private List<VelocityItemResponse> buildVelocityData(List<TaskEntity> tasks, LocalDate today) {
        List<VelocityItemResponse> list = new ArrayList<>();
        YearMonth current = YearMonth.from(today);

        for (int i = 4; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            LocalDate start = targetMonth.atDay(1);
            LocalDate end = targetMonth.atEndOfMonth();

            int created = (int) tasks.stream()
                    .filter(t -> {
                        LocalDate d = toLocalDate(t.getCreatedAt());
                        return !d.isBefore(start) && !d.isAfter(end);
                    })
                    .count();

            int completed = (int) tasks.stream()
                    .filter(t -> {
                        if (!Boolean.TRUE.equals(t.getCompleted())) {
                            return false;
                        }
                        LocalDate d = toLocalDate(t.getUpdatedAt());
                        return !d.isBefore(start) && !d.isAfter(end);
                    })
                    .count();

            String label = MONTH_NAMES[targetMonth.getMonthValue() - 1];
            list.add(new VelocityItemResponse("v-" + (5 - i), label, completed, created));
        }

        return list;
    }

    private List<CategoryDistributionResponse> buildCategoryDistribution(List<GoalEntity> goals) {
        if (goals.isEmpty()) {
            return List.of();
        }

        Map<String, Long> countMap = goals.stream()
                .collect(Collectors.groupingBy(GoalEntity::getCategory, Collectors.counting()));

        int totalGoals = goals.size();
        List<CategoryDistributionResponse> list = new ArrayList<>();
        int colorIndex = 0;

        for (Map.Entry<String, Long> entry : countMap.entrySet()) {
            int count = entry.getValue().intValue();
            int percentage = (int) Math.round(((double) count / totalGoals) * 100);
            String color = CATEGORY_COLORS[colorIndex % CATEGORY_COLORS.length];
            list.add(new CategoryDistributionResponse(
                    "cat-" + (colorIndex + 1),
                    entry.getKey(),
                    count,
                    percentage,
                    color
            ));
            colorIndex++;
        }

        return list;
    }

    private List<PriorityEfficiencyResponse> buildPriorityEfficiencies(List<TaskEntity> tasks) {
        List<PriorityEfficiencyResponse> list = new ArrayList<>();

        for (TaskPriority p : List.of(TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW)) {
            List<TaskEntity> priorityTasks = tasks.stream()
                    .filter(t -> t.getPriority() == p)
                    .toList();

            int completedCount = (int) priorityTasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getCompleted()))
                    .count();

            int activeCount = (int) priorityTasks.stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                    .count();

            long onTimeCount = priorityTasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getCompleted()))
                    .filter(t -> t.getDeadline() == null || !toLocalDate(t.getUpdatedAt()).isAfter(t.getDeadline()))
                    .count();

            int onTimePercentage = completedCount > 0
                    ? (int) Math.round(((double) onTimeCount / completedCount) * 100)
                    : 100;

            String label = switch (p) {
                case HIGH -> "Alta";
                case MEDIUM -> "Média";
                case LOW -> "Baixa";
            };

            list.add(new PriorityEfficiencyResponse(p.name(), label, completedCount, activeCount, onTimePercentage));
        }

        return list;
    }

    private List<GoalHealthItemResponse> buildGoalsHealth(List<GoalEntity> goals, LocalDate today) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return goals.stream()
                .map(goal -> {
                    List<TaskEntity> tasks = goal.getTasks();
                    int total = tasks.size();
                    int completed = (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();

                    GoalHealthStatus status = GoalHealthStatus.ON_TRACK;
                    if (goal.getDeadline() != null) {
                        if (today.isAfter(goal.getDeadline()) && goal.getProgress() < 100) {
                            status = GoalHealthStatus.DELAYED;
                        } else if (!today.plusDays(7).isBefore(goal.getDeadline()) && goal.getProgress() < 70) {
                            status = GoalHealthStatus.AT_RISK;
                        }
                    }

                    String formattedDeadline = goal.getDeadline() != null
                            ? goal.getDeadline().format(formatter)
                            : "Sem prazo";

                    return new GoalHealthItemResponse(
                            goal.getId(),
                            goal.getTitle(),
                            goal.getCategory(),
                            goal.getProgress(),
                            completed,
                            total,
                            formattedDeadline,
                            status.name().toLowerCase()
                    );
                })
                .toList();
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
