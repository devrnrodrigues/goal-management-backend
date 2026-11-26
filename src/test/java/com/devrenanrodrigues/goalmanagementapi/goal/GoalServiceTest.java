package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.activity.ActivityService;
import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;
import com.devrenanrodrigues.goalmanagementapi.common.exception.AccessForbiddenException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.AddGoalMemberRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.CreateGoalRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalMemberResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalSummaryResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.UpdateGoalMemberRoleRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.UpdateGoalRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalPriority;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMemberRepository goalMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalSecurityValidator goalSecurityValidator;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private GoalService goalService;

    private UUID userId;
    private UUID goalId;
    private UserEntity userEntity;
    private GoalEntity goalEntity;
    private GoalMemberEntity memberEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        userEntity = UserEntity.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        userEntity.setId(userId);

        goalEntity = GoalEntity.builder()
                .title("Learn Spring Boot")
                .category("Studies")
                .status(GoalStatus.PLANNING)
                .priority(GoalPriority.HIGH)
                .progress(0)
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .build();
        goalEntity.setId(goalId);
        goalEntity.setCreatedAt(Instant.now());

        memberEntity = GoalMemberEntity.builder()
                .goal(goalEntity)
                .user(userEntity)
                .role(GoalRole.OWNER)
                .build();
        memberEntity.setId(UUID.randomUUID());
        memberEntity.setCreatedAt(Instant.now());
    }

    @Test
    void shouldCreateGoalSuccessfully() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Learn Spring Boot",
                "Backend skills",
                "Studies",
                GoalPriority.HIGH,
                LocalDate.now().plusMonths(1)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(goalRepository.save(any(GoalEntity.class))).thenAnswer(invocation -> {
            GoalEntity saved = invocation.getArgument(0);
            saved.setId(goalId);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        GoalResponse response = goalService.createGoal(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Learn Spring Boot");
        assertThat(response.userRole()).isEqualTo(GoalRole.OWNER);
        assertThat(response.columns()).hasSize(4);
        verify(activityService).recordActivity(eq(userEntity), any(GoalEntity.class), any(), eq(ActivityActionType.GOAL_CREATED), eq("Learn Spring Boot"), any(), any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnCreateGoal() {
        CreateGoalRequest request = new CreateGoalRequest("Title", null, "Category", GoalPriority.MEDIUM, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.createGoal(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void shouldListUserGoalsSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<GoalEntity> pagedGoals = new PageImpl<>(List.of(goalEntity), pageable, 1);

        when(goalRepository.findAllByUserId(userId, pageable)).thenReturn(pagedGoals);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, userId)).thenReturn(Optional.of(memberEntity));

        Page<GoalSummaryResponse> result = goalService.listUserGoals(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().title()).isEqualTo("Learn Spring Boot");
        assertThat(result.getContent().getFirst().userRole()).isEqualTo(GoalRole.OWNER);
    }

    @Test
    void shouldGetGoalByIdSuccessfully() {
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(goalSecurityValidator.validateAccess(goalId, userId)).thenReturn(memberEntity);

        GoalResponse response = goalService.getGoalById(goalId, userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(goalId);
        assertThat(response.title()).isEqualTo("Learn Spring Boot");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenGoalNotFoundById() {
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoalById(goalId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Meta não encontrada.");
    }

    @Test
    void shouldUpdateGoalSuccessfully() {
        UpdateGoalRequest request = new UpdateGoalRequest(
                "Updated Title",
                "Updated Description",
                "Career",
                GoalStatus.IN_PROGRESS,
                GoalPriority.HIGH,
                LocalDate.now().plusWeeks(2)
        );

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(goalRepository.save(any(GoalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.updateGoal(goalId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        verify(activityService).recordActivity(eq(userEntity), any(GoalEntity.class), any(), eq(ActivityActionType.GOAL_UPDATED), eq("Updated Title"), any(), any());
    }

    @Test
    void shouldDeleteGoalSuccessfully() {
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));

        goalService.deleteGoal(goalId, userId);

        verify(goalSecurityValidator).validateOwner(goalId, userId);
        verify(goalRepository).delete(goalEntity);
    }

    @Test
    void shouldThrowBusinessExceptionWhenAddingMemberDirectlyAsOwner() {
        UUID newUserId = UUID.randomUUID();
        AddGoalMemberRequest request = new AddGoalMemberRequest(newUserId, GoalRole.OWNER);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));

        assertThatThrownBy(() -> goalService.addMember(goalId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é permitido adicionar um novo membro diretamente como proprietário.");
    }

    @Test
    void shouldThrowBusinessExceptionWhenUserIsAlreadyMember() {
        UUID newUserId = UUID.randomUUID();
        AddGoalMemberRequest request = new AddGoalMemberRequest(newUserId, GoalRole.VIEWER);
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(goalMemberRepository.existsByGoalIdAndUserId(goalId, newUserId)).thenReturn(true);

        assertThatThrownBy(() -> goalService.addMember(goalId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este usuário já é membro desta meta.");
    }

    @Test
    void shouldAddMemberSuccessfully() {
        UUID newUserId = UUID.randomUUID();
        UserEntity newUser = UserEntity.builder().name("Bob").email("bob@example.com").build();
        newUser.setId(newUserId);
        AddGoalMemberRequest request = new AddGoalMemberRequest(newUserId, GoalRole.VIEWER);

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goalEntity));
        when(goalMemberRepository.existsByGoalIdAndUserId(goalId, newUserId)).thenReturn(false);
        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
        when(goalMemberRepository.save(any(GoalMemberEntity.class))).thenAnswer(invocation -> {
            GoalMemberEntity m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        GoalMemberResponse response = goalService.addMember(goalId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(newUserId);
        assertThat(response.role()).isEqualTo(GoalRole.VIEWER);
    }

    @Test
    void shouldListGoalMembersSuccessfully() {
        when(goalRepository.existsById(goalId)).thenReturn(true);
        when(goalMemberRepository.findByGoalId(goalId)).thenReturn(List.of(memberEntity));

        List<GoalMemberResponse> response = goalService.listGoalMembers(goalId, userId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().userId()).isEqualTo(userId);
        verify(goalSecurityValidator).validateAccess(goalId, userId);
    }

    @Test
    void shouldThrowBusinessExceptionWhenOwnerTriesToRemoveSelf() {
        when(goalRepository.existsById(goalId)).thenReturn(true);

        assertThatThrownBy(() -> goalService.removeMember(goalId, userId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O proprietário da meta não pode ser removido dela mesma.");
    }

    @Test
    void shouldRemoveMemberSuccessfully() {
        UUID targetUserId = UUID.randomUUID();
        when(goalRepository.existsById(goalId)).thenReturn(true);
        when(goalMemberRepository.existsByGoalIdAndUserId(goalId, targetUserId)).thenReturn(true);

        goalService.removeMember(goalId, targetUserId, userId);

        verify(goalMemberRepository).deleteByGoalIdAndUserId(goalId, targetUserId);
    }

    @Test
    void shouldTransferOwnershipSuccessfullyWhenUpdatingRoleToOwner() {
        UUID targetUserId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder().name("Target").email("target@example.com").build();
        targetUser.setId(targetUserId);

        GoalMemberEntity targetMember = GoalMemberEntity.builder()
                .goal(goalEntity)
                .user(targetUser)
                .role(GoalRole.ADMIN)
                .build();
        targetMember.setId(UUID.randomUUID());
        targetMember.setCreatedAt(Instant.now());

        UpdateGoalMemberRoleRequest request = new UpdateGoalMemberRoleRequest(GoalRole.OWNER);

        when(goalRepository.existsById(goalId)).thenReturn(true);
        when(goalSecurityValidator.validateAdminOrOwner(goalId, userId)).thenReturn(memberEntity);
        when(goalMemberRepository.findByGoalIdAndUserId(goalId, targetUserId)).thenReturn(Optional.of(targetMember));
        when(goalMemberRepository.save(any(GoalMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalMemberResponse response = goalService.updateMemberRole(goalId, targetUserId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(GoalRole.OWNER);
        assertThat(memberEntity.getRole()).isEqualTo(GoalRole.ADMIN);
    }
}
