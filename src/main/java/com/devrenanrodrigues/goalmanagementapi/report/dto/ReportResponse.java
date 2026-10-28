package com.devrenanrodrigues.goalmanagementapi.report.dto;

import java.util.List;

public record ReportResponse(
    List<ReportOverviewMetric> metrics,
    List<VelocityItemResponse> velocityData,
    List<CategoryDistributionResponse> categoryDistribution,
    List<PriorityEfficiencyResponse> priorityEfficiencies,
    List<GoalHealthItemResponse> goalsHealth,
    List<String> availableCategories
) {}
