package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.common.exception.AccessForbiddenException;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalSecurityValidatorTest {

    @Mock
    private GoalMemberRepository goalMemberRepository;

    @InjectMocks
    private GoalSecurityValidator goalSecurityValidator;

    private UUID goalId;
    private UUID userId;
    private GoalMemberEntity memberEntity;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        memberEntity = GoalMemberEntity.builder()
                .role(GoalRole.VIEWER)
                .build();
    }

    @Test
    void shouldReturnMemberWhenAccessIsValid() {
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(memberEntity));

        GoalMemberEntity result = goalSecurityValidator.validateAccess(goalId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(GoalRole.VIEWER);
        verify(goalMemberRepository).findByGoalIdAndUserId(goalId, userId);
    }

    @Test
    void shouldThrowAccessForbiddenExceptionWhenMemberNotFoundOnValidateAccess() {
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalSecurityValidator.validateAccess(goalId, userId))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessage("Você não possui permissão para acessar esta meta.");

        verify(goalMemberRepository).findByGoalIdAndUserId(goalId, userId);
    }

    @Test
    void shouldPassWhenMemberIsAdminOrOwner() {
        memberEntity.setRole(GoalRole.ADMIN);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(memberEntity));

        GoalMemberEntity result = goalSecurityValidator.validateAdminOrOwner(goalId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(GoalRole.ADMIN);
    }

    @Test
    void shouldThrowAccessForbiddenExceptionWhenMemberIsNotAdminOrOwner() {
        memberEntity.setRole(GoalRole.VIEWER);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(memberEntity));

        assertThatThrownBy(() -> goalSecurityValidator.validateAdminOrOwner(goalId, userId))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessage("Você precisa de permissão de administrador ou proprietário para realizar esta alteração.");
    }

    @Test
    void shouldPassWhenMemberIsOwnerOnValidateOwner() {
        memberEntity.setRole(GoalRole.OWNER);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(memberEntity));

        GoalMemberEntity result = goalSecurityValidator.validateOwner(goalId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(GoalRole.OWNER);
    }

    @Test
    void shouldThrowAccessForbiddenExceptionWhenMemberIsNotOwnerOnValidateOwner() {
        memberEntity.setRole(GoalRole.ADMIN);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(memberEntity));

        assertThatThrownBy(() -> goalSecurityValidator.validateOwner(goalId, userId))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessage("Apenas o proprietário da meta pode realizar esta ação.");
    }
}
