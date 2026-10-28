package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.activity.ActivityService;
import com.devrenanrodrigues.goalmanagementapi.activity.enums.ActivityActionType;
import com.devrenanrodrigues.goalmanagementapi.board.BoardColumnEntity;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.AddGoalMemberRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.CreateGoalRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalMemberResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.GoalSummaryResponse;
import com.devrenanrodrigues.goalmanagementapi.goal.dto.UpdateGoalRequest;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalRole;
import com.devrenanrodrigues.goalmanagementapi.goal.enums.GoalStatus;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final UserRepository userRepository;
    private final GoalSecurityValidator goalSecurityValidator;
    private final ActivityService activityService;

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request, UUID currentUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        GoalEntity goal = GoalEntity.builder()
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .category(request.category().trim())
                .status(GoalStatus.PLANNING)
                .priority(request.priority())
                .deadline(request.deadline())
                .progress(0)
                .build();

        GoalMemberEntity ownerMember = GoalMemberEntity.builder()
                .goal(goal)
                .user(currentUser)
                .role(GoalRole.OWNER)
                .build();
        goal.getMembers().add(ownerMember);

        BoardColumnEntity colBacklog = BoardColumnEntity.builder().goal(goal).title("Backlog").color("#64748b").position(0).build();
        BoardColumnEntity colTodo = BoardColumnEntity.builder().goal(goal).title("A Fazer").color("#3b82f6").position(1).build();
        BoardColumnEntity colProgress = BoardColumnEntity.builder().goal(goal).title("Em progresso").color("#f59e0b").position(2).build();
        BoardColumnEntity colDone = BoardColumnEntity.builder().goal(goal).title("Concluído").color("#10b981").position(3).build();

        goal.getColumns().addAll(List.of(colBacklog, colTodo, colProgress, colDone));

        GoalEntity savedGoal = goalRepository.save(goal);

        activityService.recordActivity(
                currentUser,
                savedGoal,
                null,
                ActivityActionType.GOAL_CREATED,
                savedGoal.getTitle(),
                null,
                null
        );

        return GoalResponse.of(savedGoal, GoalRole.OWNER);
    }

    @Transactional(readOnly = true)
    public Page<GoalSummaryResponse> listUserGoals(UUID currentUserId, Pageable pageable) {
        return goalRepository.findAllByUserId(currentUserId, pageable)
                .map(goal -> {
                    GoalRole role = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), currentUserId)
                            .map(GoalMemberEntity::getRole)
                            .orElse(GoalRole.VIEWER);
                    return GoalSummaryResponse.of(goal, role);
                });
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(UUID goalId, UUID currentUserId) {
        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));
        GoalMemberEntity member = goalSecurityValidator.validateAccess(goalId, currentUserId);
        return GoalResponse.of(goal, member.getRole());
    }

    @Transactional
    public GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, UUID currentUserId) {
        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));
        GoalMemberEntity member = goalSecurityValidator.validateAdminOrOwner(goalId, currentUserId);

        goal.setTitle(request.title().trim());
        goal.setDescription(request.description() != null ? request.description().trim() : null);
        goal.setCategory(request.category().trim());
        goal.setStatus(request.status());
        goal.setPriority(request.priority());
        goal.setDeadline(request.deadline());

        GoalEntity updatedGoal = goalRepository.save(goal);

        activityService.recordActivity(
                member.getUser(),
                updatedGoal,
                null,
                ActivityActionType.GOAL_UPDATED,
                updatedGoal.getTitle(),
                null,
                null
        );

        return GoalResponse.of(updatedGoal, member.getRole());
    }

    @Transactional
    public void deleteGoal(UUID goalId, UUID currentUserId) {
        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));
        goalSecurityValidator.validateOwner(goalId, currentUserId);
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalMemberResponse addMember(UUID goalId, AddGoalMemberRequest request, UUID currentUserId) {
        GoalEntity goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));
        goalSecurityValidator.validateAdminOrOwner(goalId, currentUserId);

        if (goalMemberRepository.existsByGoalIdAndUserId(goalId, request.userId())) {
            throw new BusinessException("Este usuário já é membro desta meta.");
        }

        UserEntity userToAdd = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário informado não existe."));

        GoalMemberEntity newMember = GoalMemberEntity.builder()
                .goal(goal)
                .user(userToAdd)
                .role(request.role())
                .build();

        GoalMemberEntity savedMember = goalMemberRepository.save(newMember);
        return GoalMemberResponse.fromEntity(savedMember);
    }

    @Transactional(readOnly = true)
    public List<GoalMemberResponse> listGoalMembers(UUID goalId, UUID currentUserId) {
        if (!goalRepository.existsById(goalId)) {
            throw new ResourceNotFoundException("Meta não encontrada.");
        }
        goalSecurityValidator.validateAccess(goalId, currentUserId);
        return goalMemberRepository.findByGoalId(goalId)
                .stream()
                .map(GoalMemberResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void removeMember(UUID goalId, UUID targetUserId, UUID currentUserId) {
        if (!goalRepository.existsById(goalId)) {
            throw new ResourceNotFoundException("Meta não encontrada.");
        }
        goalSecurityValidator.validateOwner(goalId, currentUserId);

        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("O proprietário da meta não pode ser removido dela mesma.");
        }

        if (!goalMemberRepository.existsByGoalIdAndUserId(goalId, targetUserId)) {
            throw new ResourceNotFoundException("O vínculo do membro não foi encontrado nesta meta.");
        }

        goalMemberRepository.deleteByGoalIdAndUserId(goalId, targetUserId);
    }
}
