package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnEntity;
import com.devrenanrodrigues.goalmanagementapi.common.entity.BaseAuditableEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalPriority;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "goals")
public class GoalEntity extends BaseAuditableEntity {

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private GoalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private GoalPriority priority;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "progress", nullable = false)
    private Integer progress;

    @Builder.Default
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalMemberEntity> members = new ArrayList<>();

    @Builder.Default
    @OrderBy("position ASC")
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardColumnEntity> columns = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();
}
