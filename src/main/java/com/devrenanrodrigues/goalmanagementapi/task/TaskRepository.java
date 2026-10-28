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

    java.util.Optional<TaskEntity> findFirstByGoalIdAndCompletedFalseOrderByPositionAsc(UUID goalId);

    @Query("""
        SELECT COUNT(t) FROM TaskEntity t
        JOIN t.goal g
        JOIN g.members m
        WHERE m.user.id = :userId AND t.deadline = :today
    """)
    long countTasksDueToday(@Param("userId") UUID userId, @Param("today") java.time.LocalDate today);

    @Query("""
        SELECT COUNT(t) FROM TaskEntity t
        JOIN t.goal g
        JOIN g.members m
        WHERE m.user.id = :userId AND t.deadline = :today AND t.completed = true
    """)
    long countTasksCompletedToday(@Param("userId") UUID userId, @Param("today") java.time.LocalDate today);

    @Query("""
        SELECT t FROM TaskEntity t
        JOIN t.goal g
        JOIN g.members m
        WHERE m.user.id = :userId
    """)
    java.util.List<TaskEntity> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT t FROM TaskEntity t
        JOIN FETCH t.goal g
        JOIN FETCH t.column c
        JOIN g.members gm
        WHERE gm.user.id = :userId
        AND (
            (t.deadline IS NOT NULL AND t.deadline BETWEEN :startDate AND :endDate)
            OR
            (t.deadline IS NULL AND t.createdAt >= :startInstant AND t.createdAt <= :endInstant)
        )
        ORDER BY t.deadline ASC NULLS LAST, t.createdAt ASC
    """)
    java.util.List<TaskEntity> findCalendarTasks(
        @Param("userId") UUID userId,
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate,
        @Param("startInstant") java.time.Instant startInstant,
        @Param("endInstant") java.time.Instant endInstant
    );
}
