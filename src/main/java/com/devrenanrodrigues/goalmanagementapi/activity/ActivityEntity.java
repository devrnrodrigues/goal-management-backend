package com.devrenanrodrigues.goalmanagementapi.activity;

import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;
import com.devrenanrodrigues.goalmanagementapi.common.entity.BaseAuditableEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activities")
public class ActivityEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private GoalEntity goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActivityActionType actionType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "from_column", length = 80)
    private String fromColumn;

    @Column(name = "to_column", length = 80)
    private String toColumn;
}
