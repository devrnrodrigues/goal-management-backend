package com.devrenanrodrigues.goalmanagementapi.goal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    @Query("""
        SELECT g FROM GoalEntity g
        JOIN g.members m
        WHERE m.user.id = :userId
        ORDER BY g.createdAt DESC
    """)
    Page<GoalEntity> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT g FROM GoalEntity g
        JOIN g.members m
        WHERE g.id = :goalId AND m.user.id = :userId
    """)
    Optional<GoalEntity> findByIdAndUserId(@Param("goalId") UUID goalId, @Param("userId") UUID userId);

    @Query("""
        SELECT g FROM GoalEntity g
        JOIN g.members m
        WHERE m.user.id = :userId
    """)
    java.util.List<GoalEntity> findAllByMembersUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT g FROM GoalEntity g
        JOIN g.members m
        WHERE m.user.id = :userId AND g.status != com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus.COMPLETED
        ORDER BY g.deadline ASC NULLS LAST, g.createdAt DESC
    """)
    java.util.List<GoalEntity> findFocusGoalsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
