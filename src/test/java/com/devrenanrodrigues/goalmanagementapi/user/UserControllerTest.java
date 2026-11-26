package com.devrenanrodrigues.goalmanagementapi.user;

import com.devrenanrodrigues.goalmanagementapi.common.exception.GlobalExceptionHandler;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UpdateProfileRequest;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, UserControllerTest.TestWebMvcConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    private UUID userId;
    private AuthenticatedUser authenticatedUser;
    private UserResponse userResponse;

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
        authenticatedUser = new AuthenticatedUser(userId, "john.doe@example.com", "John Doe");
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        userResponse = new UserResponse(
                userId,
                "John Doe",
                "john.doe@example.com",
                "https://avatar.url/john.png",
                AuthProvider.LOCAL,
                "LIGHT",
                true,
                true,
                Instant.now()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserSuccessfully() throws Exception {
        when(userService.getCurrentUser(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.theme").value("LIGHT"));

        verify(userService).getCurrentUser(userId);
    }

    @Test
    void shouldReturnNotFoundWhenCurrentUserDoesNotExist() throws Exception {
        when(userService.getCurrentUser(userId))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado."));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado."));

        verify(userService).getCurrentUser(userId);
    }

    @Test
    void shouldUpdateProfileSuccessfullyWhenPayloadIsValid() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Updated",
                "https://avatar.url/updated.png",
                "DARK",
                false,
                false,
                null,
                null
        );

        UserResponse updatedResponse = new UserResponse(
                userId,
                "John Updated",
                "john.doe@example.com",
                "https://avatar.url/updated.png",
                AuthProvider.LOCAL,
                "DARK",
                false,
                false,
                userResponse.createdAt()
        );

        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.emailNotifications").value(false));

        verify(userService).updateProfile(eq(userId), any(UpdateProfileRequest.class));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenNameIsBlank() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório."));
    }

    @Test
    void shouldListUsersSuccessfullyWithPagination() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> pagedResponse = new PageImpl<>(List.of(userResponse), pageable, 1);

        when(userService.listUsers(eq("John"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/users")
                        .param("search", "John")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).listUsers(eq("John"), any(Pageable.class));
    }
}
