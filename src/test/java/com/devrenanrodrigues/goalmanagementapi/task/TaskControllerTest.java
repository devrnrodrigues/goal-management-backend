package com.devrenanrodrigues.goalmanagementapi.task;

import com.devrenanrodrigues.goalmanagementapi.common.exception.GlobalExceptionHandler;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
import com.devrenanrodrigues.goalmanagementapi.task.dto.CreateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.MoveTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.dto.TaskResponse;
import com.devrenanrodrigues.goalmanagementapi.task.dto.UpdateTaskRequest;
import com.devrenanrodrigues.goalmanagementapi.task.enums.TaskPriority;
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

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TaskControllerTest.TestWebMvcConfig.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    private UUID userId;
    private UUID goalId;
    private UUID taskId;
    private TaskResponse taskResponse;

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
        taskId = UUID.randomUUID();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "alice@example.com", "Alice");
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        taskResponse = new TaskResponse(
                taskId,
                goalId,
                UUID.randomUUID(),
                "A Fazer",
                "Write tests",
                "Backend coverage",
                false,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(3),
                0,
                0,
                0,
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
    void shouldListTasksSuccessfully() throws Exception {
        when(taskService.listTasksByGoal(goalId, userId)).thenReturn(List.of(taskResponse));

        mockMvc.perform(get("/api/goals/{goalId}/tasks", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].title").value("Write tests"));

        verify(taskService).listTasksByGoal(goalId, userId);
    }

    @Test
    void shouldCreateTaskSuccessfully() throws Exception {
        when(taskService.createTask(eq(goalId), any(CreateTaskRequest.class), eq(userId))).thenReturn(taskResponse);

        mockMvc.perform(post("/api/goals/{goalId}/tasks", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write tests",
                                  "description": "Backend coverage",
                                  "priority": "HIGH",
                                  "deadline": "2026-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Write tests"));

        verify(taskService).createTask(eq(goalId), any(CreateTaskRequest.class), eq(userId));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenTitleIsBlankOnCreateTask() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/tasks", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": ""
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.title").value("O título da tarefa é obrigatório."));
    }

    @Test
    void shouldUpdateTaskSuccessfully() throws Exception {
        when(taskService.updateTask(eq(taskId), any(UpdateTaskRequest.class), eq(userId))).thenReturn(taskResponse);

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "description": "New Description",
                                  "priority": "LOW",
                                  "deadline": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk());

        verify(taskService).updateTask(eq(taskId), any(UpdateTaskRequest.class), eq(userId));
    }

    @Test
    void shouldMoveTaskSuccessfully() throws Exception {
        UUID targetColumnId = UUID.randomUUID();
        MoveTaskRequest request = new MoveTaskRequest(targetColumnId, 1);

        when(taskService.moveTask(eq(taskId), any(MoveTaskRequest.class), eq(userId))).thenReturn(taskResponse);

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(taskService).moveTask(eq(taskId), any(MoveTaskRequest.class), eq(userId));
    }

    @Test
    void shouldToggleTaskCompleteSuccessfully() throws Exception {
        when(taskService.toggleTaskComplete(taskId, userId)).thenReturn(taskResponse);

        mockMvc.perform(patch("/api/tasks/{taskId}/toggle-complete", taskId))
                .andExpect(status().isOk());

        verify(taskService).toggleTaskComplete(taskId, userId);
    }

    @Test
    void shouldDeleteTaskSuccessfully() throws Exception {
        doNothing().when(taskService).deleteTask(taskId, userId);

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(taskId, userId);
    }
}
