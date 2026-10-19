package com.devrenanrodrigues.goalmanagementapi.task;

import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnEntity;
import com.devrenanrodrigues.goalmanagementapi.common.entity.BaseAuditableEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks")
public class TaskEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private GoalEntity goal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumnEntity column;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TaskPriority priority;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "attachments_count", nullable = false)
    private Integer attachmentsCount;

    @Column(name = "comments_count", nullable = false)
    private Integer commentsCount;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_assigned_members",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> assignedMembers = new HashSet<>();
}
