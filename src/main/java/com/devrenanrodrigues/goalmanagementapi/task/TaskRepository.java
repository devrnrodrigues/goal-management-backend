package com.devrenanrodrigues.goalmanagementapi.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByGoalIdOrderByPositionAsc(UUID goalId);

    List<TaskEntity> findByColumnId(UUID columnId);

    int countByGoalId(UUID goalId);

    int countByGoalIdAndCompletedTrue(UUID goalId);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.goal.id = :goalId AND t.column.id = :columnId")
    int countByGoalIdAndColumnId(@Param("goalId") UUID goalId, @Param("columnId") UUID columnId);
}
