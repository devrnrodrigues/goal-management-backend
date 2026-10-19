package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.common.exception.AccessForbiddenException;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoalSecurityValidator {

    private final GoalMemberRepository goalMemberRepository;

    public GoalMemberEntity validateAccess(UUID goalId, UUID userId) {
        return goalMemberRepository.findByGoalIdAndUserId(goalId, userId)
                .orElseThrow(() -> new AccessForbiddenException("Você não possui permissão para acessar esta meta."));
    }

    public GoalMemberEntity validateAdminOrOwner(UUID goalId, UUID userId) {
        GoalMemberEntity member = validateAccess(goalId, userId);
        if (member.getRole() != GoalRole.OWNER && member.getRole() != GoalRole.ADMIN) {
            throw new AccessForbiddenException("Você precisa de permissão de administrador ou proprietário para realizar esta alteração.");
        }
        return member;
    }

    public GoalMemberEntity validateOwner(UUID goalId, UUID userId) {
        GoalMemberEntity member = validateAccess(goalId, userId);
        if (member.getRole() != GoalRole.OWNER) {
            throw new AccessForbiddenException("Apenas o proprietário da meta pode realizar esta ação.");
        }
        return member;
    }
}
