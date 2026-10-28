package com.devrenanrodrigues.goalmanagementapi.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, UUID> {

    @Query("SELECT a FROM ActivityEntity a WHERE a.goal.id = :goalId ORDER BY a.createdAt DESC")
    Page<ActivityEntity> findByGoalId(@Param("goalId") UUID goalId, Pageable pageable);

    @Query("""
        SELECT a FROM ActivityEntity a
        JOIN GoalMemberEntity gm ON gm.goal.id = a.goal.id
        WHERE gm.user.id = :userId
        ORDER BY a.createdAt DESC
    """)
    Page<ActivityEntity> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a.createdAt FROM ActivityEntity a WHERE a.author.id = :userId ORDER BY a.createdAt DESC")
    java.util.List<java.time.Instant> findActivityInstantsByUserId(@Param("userId") UUID userId);
}
