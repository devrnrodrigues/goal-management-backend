package com.devrenanrodrigues.goalmanagementapi.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalMemberRepository extends JpaRepository<GoalMemberEntity, UUID> {

    Optional<GoalMemberEntity> findByGoalIdAndUserId(UUID goalId, UUID userId);

    List<GoalMemberEntity> findByGoalId(UUID goalId);

    boolean existsByGoalIdAndUserId(UUID goalId, UUID userId);

    void deleteByGoalIdAndUserId(UUID goalId, UUID userId);
}
