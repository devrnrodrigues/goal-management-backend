package com.devrenanrodrigues.goalmanagementapi.auth;

import com.devrenanrodrigues.goalmanagementapi.auth.dto.AuthResponse;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.GoogleLoginRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.LoginRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.RefreshTokenRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.RegisterRequest;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.GlobalExceptionHandler;
import com.devrenanrodrigues.goalmanagementapi.common.exception.UnauthorizedException;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(
                userId,
                "Jane Doe",
                "jane.doe@example.com",
                null,
                AuthProvider.LOCAL,
                "LIGHT",
                true,
                true,
                Instant.now()
        );
        authResponse = AuthResponse.of("mockedAccessToken", "mockedRefreshToken", userResponse);
    }

    @Test
    void shouldRegisterSuccessfullyWhenPayloadIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane.doe@example.com", "password123");
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mockedAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("mockedRefreshToken"))
                .andExpect(jsonPath("$.user.email").value("jane.doe@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenRegisterPayloadIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldLoginSuccessfullyWhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest("jane.doe@example.com", "password123");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockedAccessToken"))
                .andExpect(jsonPath("$.user.name").value("Jane Doe"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenLoginFailsWithInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("jane.doe@example.com", "wrongPassword");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException("E-mail ou senha inválidos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldLoginWithGoogleSuccessfullyWhenTokenIsValid() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("validGoogleToken");
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockedAccessToken"));

        verify(authService).loginWithGoogle(any(GoogleLoginRequest.class));
    }

    @Test
    void shouldRefreshTokenSuccessfullyWhenTokenIsValid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("mockedRefreshToken"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenIsExpired() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UnauthorizedException("Sessão expirada. Faça login novamente."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Sessão expirada. Faça login novamente."));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("activeRefreshToken");
        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshTokenRequest.class));
    }
}
