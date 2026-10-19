package com.devrenanrodrigues.goalmanagementapi.board;

import com.devrenanrodrigues.goalmanagementapi.common.entity.BaseAuditableEntity;
import com.devrenanrodrigues.goalmanagementapi.goal.GoalEntity;
import com.devrenanrodrigues.goalmanagementapi.task.TaskEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "board_columns")
public class BoardColumnEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private GoalEntity goal;

    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Builder.Default
    @OrderBy("position ASC")
    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL)
    private List<TaskEntity> tasks = new ArrayList<>();
}
