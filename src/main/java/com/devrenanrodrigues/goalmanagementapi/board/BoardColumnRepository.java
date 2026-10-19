package com.devrenanrodrigues.goalmanagementapi.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumnEntity, UUID> {

    List<BoardColumnEntity> findByGoalIdOrderByPositionAsc(UUID goalId);

    Optional<BoardColumnEntity> findFirstByGoalIdAndIdNotOrderByPositionAsc(UUID goalId, UUID columnId);

    int countByGoalId(UUID goalId);
}
