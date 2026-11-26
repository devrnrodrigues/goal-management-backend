package com.devrenanrodrigues.goalmanagementapi.goal;

import com.devrenanrodrigues.goalmanagementapi.common.exception.GlobalExceptionHandler;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
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
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GoalControllerTest.TestWebMvcConfig.class})
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    private UUID userId;
    private UUID goalId;
    private GoalResponse goalResponse;

    @TestConfiguration
    static class TestWebMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.getParameterType().equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    return auth != null ? auth.getPrincipal() : null;
                }
            });
        }
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "alice@example.com", "Alice");
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        goalResponse = new GoalResponse(
                goalId,
                "Learn Spring Boot",
                "Backend mastery",
                "Studies",
                GoalStatus.PLANNING,
                GoalPriority.HIGH,
                LocalDate.now().plusMonths(1),
                0,
                GoalRole.OWNER,
                Collections.emptyList(),
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateGoalSuccessfully() throws Exception {
        when(goalService.createGoal(any(CreateGoalRequest.class), eq(userId))).thenReturn(goalResponse);

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Learn Spring Boot",
                                  "description": "Backend mastery",
                                  "category": "Studies",
                                  "priority": "HIGH",
                                  "deadline": "2026-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(goalId.toString()))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"));

        verify(goalService).createGoal(any(CreateGoalRequest.class), eq(userId));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenTitleIsBlankOnCreateGoal() throws Exception {
        CreateGoalRequest request = new CreateGoalRequest(
                "",
                null,
                "Studies",
                GoalPriority.HIGH,
                null
        );

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.title").value("O título da meta é obrigatório."));
    }

    @Test
    void shouldListGoalsSuccessfully() throws Exception {
        GoalSummaryResponse summaryResponse = new GoalSummaryResponse(
                goalId,
                "Learn Spring Boot",
                null,
                "Studies",
                GoalStatus.PLANNING,
                GoalPriority.HIGH,
                LocalDate.now().plusMonths(1),
                0,
                1,
                GoalRole.OWNER,
                Instant.now(),
                Collections.emptyList()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<GoalSummaryResponse> pagedResponse = new PageImpl<>(List.of(summaryResponse), pageable, 1);

        when(goalService.listUserGoals(eq(userId), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/goals")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(goalService).listUserGoals(eq(userId), any(Pageable.class));
    }

    @Test
    void shouldGetGoalByIdSuccessfully() throws Exception {
        when(goalService.getGoalById(goalId, userId)).thenReturn(goalResponse);

        mockMvc.perform(get("/api/goals/{id}", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()));

        verify(goalService).getGoalById(goalId, userId);
    }

    @Test
    void shouldReturnNotFoundWhenGoalDoesNotExist() throws Exception {
        when(goalService.getGoalById(goalId, userId))
                .thenThrow(new ResourceNotFoundException("Meta não encontrada."));

        mockMvc.perform(get("/api/goals/{id}", goalId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(goalService).getGoalById(goalId, userId);
    }

    @Test
    void shouldUpdateGoalSuccessfully() throws Exception {
        when(goalService.updateGoal(eq(goalId), any(UpdateGoalRequest.class), eq(userId))).thenReturn(goalResponse);

        mockMvc.perform(put("/api/goals/{id}", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "description": "New Description",
                                  "category": "Career",
                                  "status": "IN_PROGRESS",
                                  "priority": "HIGH",
                                  "deadline": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk());

        verify(goalService).updateGoal(eq(goalId), any(UpdateGoalRequest.class), eq(userId));
    }

    @Test
    void shouldDeleteGoalSuccessfully() throws Exception {
        doNothing().when(goalService).deleteGoal(goalId, userId);

        mockMvc.perform(delete("/api/goals/{id}", goalId))
                .andExpect(status().isNoContent());

        verify(goalService).deleteGoal(goalId, userId);
    }

    @Test
    void shouldAddMemberSuccessfully() throws Exception {
        UUID newUserId = UUID.randomUUID();
        AddGoalMemberRequest request = new AddGoalMemberRequest(newUserId, GoalRole.VIEWER);
        GoalMemberResponse memberResponse = new GoalMemberResponse(
                UUID.randomUUID(),
                newUserId,
                "Bob",
                "bob@example.com",
                null,
                GoalRole.VIEWER,
                Instant.now()
        );

        when(goalService.addMember(eq(goalId), any(AddGoalMemberRequest.class), eq(userId)))
                .thenReturn(memberResponse);

        mockMvc.perform(post("/api/goals/{id}/members", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(newUserId.toString()))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        verify(goalService).addMember(eq(goalId), any(AddGoalMemberRequest.class), eq(userId));
    }

    @Test
    void shouldListMembersSuccessfully() throws Exception {
        GoalMemberResponse memberResponse = new GoalMemberResponse(
                UUID.randomUUID(),
                userId,
                "Alice",
                "alice@example.com",
                null,
                GoalRole.OWNER,
                Instant.now()
        );

        when(goalService.listGoalMembers(goalId, userId)).thenReturn(List.of(memberResponse));

        mockMvc.perform(get("/api/goals/{id}/members", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));

        verify(goalService).listGoalMembers(goalId, userId);
    }

    @Test
    void shouldRemoveMemberSuccessfully() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        doNothing().when(goalService).removeMember(goalId, targetUserId, userId);

        mockMvc.perform(delete("/api/goals/{id}/members/{userId}", goalId, targetUserId))
                .andExpect(status().isNoContent());

        verify(goalService).removeMember(goalId, targetUserId, userId);
    }

    @Test
    void shouldUpdateMemberRoleSuccessfully() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UpdateGoalMemberRoleRequest request = new UpdateGoalMemberRoleRequest(GoalRole.ADMIN);
        GoalMemberResponse updatedResponse = new GoalMemberResponse(
                UUID.randomUUID(),
                targetUserId,
                "Bob",
                "bob@example.com",
                null,
                GoalRole.ADMIN,
                Instant.now()
        );

        when(goalService.updateMemberRole(eq(goalId), eq(targetUserId), any(UpdateGoalMemberRoleRequest.class), eq(userId)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/goals/{id}/members/{userId}", goalId, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(goalService).updateMemberRole(eq(goalId), eq(targetUserId), any(UpdateGoalMemberRoleRequest.class), eq(userId));
    }
}
