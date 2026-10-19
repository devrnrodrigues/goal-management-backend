package com.devrenanrodrigues.goalmanagementapi.activity;

import com.devrenanrodrigues.goalmanagementapi.activity.dto.ActivityResponse;
import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Transactional
    public void recordActivity(
            UserEntity author,
            GoalEntity goal,
            TaskEntity task,
            ActivityActionType actionType,
            String title,
            String fromColumn,
            String toColumn
    ) {
        ActivityEntity activity = ActivityEntity.builder()
                .author(author)
                .goal(goal)
                .task(task)
                .actionType(actionType)
                .title(title)
                .fromColumn(fromColumn)
                .toColumn(toColumn)
                .build();

        activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listUserActivities(UUID userId, Pageable pageable) {
        return activityRepository.findRecentByUserId(userId, pageable)
                .map(ActivityResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listGoalActivities(UUID goalId, Pageable pageable) {
        return activityRepository.findByGoalId(goalId, pageable)
                .map(ActivityResponse::fromEntity);
    }
}
